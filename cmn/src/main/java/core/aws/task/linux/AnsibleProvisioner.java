package core.aws.task.linux;

import com.amazonaws.services.ec2.model.Instance;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import core.aws.env.Environment;
import core.aws.resource.ec2.KeyPair;
import core.aws.util.Files;
import core.aws.util.Maps;
import core.aws.util.SSH;
import core.aws.util.Strings;
import core.aws.util.Tarball;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.delete;

/**
 * @author neo
 */
public class AnsibleProvisioner {
    private final Logger logger = LoggerFactory.getLogger(AnsibleProvisioner.class);

    private final Environment env;
    private final Instance instance;
    private final Path playbookPath;
    private final Optional<Path> packageDir;
    public Map<String, String> additionalVariables = Maps.newHashMap();

    public AnsibleProvisioner(Environment env, Instance instance, Path playbookPath, Optional<Path> packageDir) {
        this.env = env;
        this.instance = instance;
        this.playbookPath = playbookPath;
        this.packageDir = packageDir;
    }

    public void provision() throws IOException, JSchException, InterruptedException, SftpException {
        logger.info("playbook => {}", playbookPath);
        logger.info("packageDir => {}", packageDir);

        try (SSH ssh = new SSH(hostName(instance), "ubuntu", KeyPair.keyFile(instance.getKeyName(), env))) {
            ssh.executeCommands("sudo apt-get -y -q update",
                    "dpkg -s ansible || sudo apt-get -y install software-properties-common && sudo apt-add-repository ppa:ansible/ansible -y && sudo apt-get -y update && sudo apt-get -y install ansible",
                    "sudo DEBIAN_FRONTEND=noninteractive apt-get -y --force-yes -q -o Dpkg::Options::='--force-confdef' -o Dpkg::Options::='--force-confold' dist-upgrade", // update package before run playbook to make sure ansible is updated in advance
                    "sudo rm -rf /opt/ansible /opt/packages",    // clear previous ansible roles and packages if installed
                    "sudo mkdir -p /opt/packages /opt/ansible",
                    "sudo chown ubuntu.ubuntu /opt/packages /opt/ansible");

            uploadPackage(ssh);

            runPlaybook(ssh);
        }
    }

    private void runPlaybook(SSH ssh) throws InterruptedException, JSchException, IOException, SftpException {
        Path ansibleArchive = packAnsibleRoles();
        ssh.put(ansibleArchive, "/tmp/ansible.tar.gz");
        ssh.put(playbookPath, "/opt/ansible/localhost.yml");

        ssh.executeCommands("tar xzf /tmp/ansible.tar.gz -C /opt/ansible",
                "ansible-playbook --become /opt/ansible/localhost.yml");

        delete(ansibleArchive);
    }

    private Path packAnsibleRoles() throws IOException {
        Path ansiblePath = createTempDirectory("ansible");
        for (Path path : env.ansibleRolePaths) {
            Files.copyDirectory(path, ansiblePath);
        }

        Tarball tar = new Tarball(ansiblePath);
        File tarFile = createTempFile("ansible.tar.gz", null).toFile();
        tar.archive(tarFile);
        Files.deleteDirectory(ansiblePath);

        return tarFile.toPath();
    }

    private void uploadPackage(SSH ssh) {
        packageDir.ifPresent(path -> ssh.uploadDir(path, "/opt/packages"));
    }

    private String hostName(Instance instance) {
        String publicDNS = instance.getPublicDnsName();
        return Strings.notEmpty(publicDNS) ? publicDNS : instance.getPrivateIpAddress();
    }
}
