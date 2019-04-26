package core.aws.util;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Properties;

import static java.nio.file.Files.isDirectory;
import static java.nio.file.Files.walkFileTree;

/**
 * @author neo
 */
public final class SSH implements Closeable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Logger messageLogger = LoggerFactory.getLogger("message");

    private final String host;
    private final String user;
    private final Path privateKey;
    private Session session;

    public SSH(String host, String user, Path privateKey) {
        this.host = host;
        this.user = user;
        this.privateKey = privateKey;
    }

    @Override
    public void close() {
        session.disconnect();
    }

    public void executeCommands(String... commands) throws JSchException, IOException, InterruptedException {
        connectIfNot();
        for (String command : commands) {
            executeCommand(command);
        }
    }

    public void put(Path localPath, String remotePath) throws JSchException, SftpException {
        connectIfNot();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        try {
            channel.connect();
            logger.info("sftp put, from={}, to={}", localPath, remotePath);
            channel.put(new ByteArrayInputStream(Files.bytes(localPath)), remotePath);
        } finally {
            channel.disconnect();
        }
    }

    // copy all files recursively from local dir to remote dir, e.g. copy local env/packages to /opt/packages
    public void uploadDir(final Path localDir, final String remoteDir) {
        Asserts.isTrue(isDirectory(localDir), "localDir must be directory, localDir={}", localDir);
        ChannelSftp channel = null;
        try {
            connectIfNot();
            channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();
            walkFileTree(localDir, new PutFileVisitor(localDir, remoteDir, channel));
        } catch (JSchException | IOException e) {
            throw new SSHException(e);
        } finally {
            if (channel != null)
                channel.disconnect();
        }
    }

    private void executeCommand(String command) throws JSchException, IOException, InterruptedException {
        connectIfNot();
        Channel channel = session.openChannel("exec");
        try {
            ((ChannelExec) channel).setCommand(command);
            ((ChannelExec) channel).setErrStream(System.err);
            ((ChannelExec) channel).setPty(true);
            ((ChannelExec) channel).setPtyType("vt100");
            channel.setInputStream(null);
            channel.setOutputStream(System.out);
            InputStream in = channel.getInputStream();
            logger.info("ssh exec command => {}", command);
            channel.connect();

            byte[] buffer = new byte[1024];
            while (true) {
                while (in.available() > 0) {
                    int i = in.read(buffer, 0, 1024);
                    if (i < 0) break;
                    messageLogger.info(new String(buffer, 0, i, Charsets.UTF_8));
                }
                if (channel.isClosed()) {
                    logger.info("ssh exec exit status => " + channel.getExitStatus());
                    break;
                }

                Thread.sleep(1000);
            }

            if (channel.getExitStatus() != 0) {
                throw new JSchException("failed to run command, command=" + command);
            }
        } finally {
            channel.disconnect();
        }
    }

    private void connectIfNot() throws JSchException {
        if (session == null) {
            JSch jsch = new JSch();
            jsch.addIdentity(privateKey.toAbsolutePath().toString());
            session = jsch.getSession(user, host, 22);
            Properties config = new Properties();
            config.setProperty("StrictHostKeyChecking", "no");
            session.setConfig(config);
            logger.info("ssh connect to {}", host);
            session.connect();
        }
    }

    public static class SSHException extends RuntimeException {
        private static final long serialVersionUID = 3248580184982451829L;

        SSHException(Throwable cause) {
            super(cause);
        }
    }

    private class PutFileVisitor extends SimpleFileVisitor<Path> {
        private final String remoteDir;
        private final Path localDir;
        private final ChannelSftp channel;

        PutFileVisitor(Path localDir, String remoteDir, ChannelSftp channel) {
            this.remoteDir = remoteDir;
            this.localDir = localDir;
            this.channel = channel;
        }

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attributes) throws IOException {
            try {
                String targetDir = remoteDir + "/" + toLinuxPath(localDir.relativize(dir));
                if (!remoteDirExists(targetDir)) {
                    logger.info("sftp mkdir, remoteDir={}", targetDir);
                    channel.mkdir(targetDir);
                }
                return FileVisitResult.CONTINUE;
            } catch (SftpException e) {
                throw new IOException(e);
            }
        }

        private boolean remoteDirExists(String remoteDir) {
            try {
                SftpATTRS stat = channel.stat(remoteDir);
                return stat.isDir();
            } catch (SftpException e) {
                return false;
            }
        }

        private String toLinuxPath(Path path) {
            return path.toString().replace("\\", "/");    // convert windows file separator to linux one
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException {
            try {
                String source = file.toAbsolutePath().toString();
                String destination = remoteDir + "/" + toLinuxPath(localDir.relativize(file));
                logger.info("sftp put, source={}, destination={}", source, destination);
                channel.put(source, destination);
                return FileVisitResult.CONTINUE;
            } catch (SftpException e) {
                throw new IOException(e);
            }
        }
    }
}
