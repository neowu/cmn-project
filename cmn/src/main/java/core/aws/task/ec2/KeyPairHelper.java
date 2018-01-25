package core.aws.task.ec2;

import com.amazonaws.services.ec2.model.KeyPairInfo;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.ec2.KeyPair;
import core.aws.util.Charsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import java.util.Locale;

/**
 * @author neo
 */
public class KeyPairHelper {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Environment env;

    public KeyPairHelper(Environment env) {
        this.env = env;
    }

    public void createKeyPair(KeyPair keyPair) throws IOException {
        com.amazonaws.services.ec2.model.KeyPair remoteKeyPair = AWS.getEc2().createKeyPair(keyPair.name);
        writeKeyFile(keyPair.name, remoteKeyPair.getKeyMaterial());
        keyPair.remoteKeyPair = new KeyPairInfo()
            .withKeyName(remoteKeyPair.getKeyName())
            .withKeyFingerprint(remoteKeyPair.getKeyFingerprint());
    }

    private void writeKeyFile(String keyName, String content) throws IOException {
        Path keyFile = KeyPair.keyFile(keyName, env);
        Files.createDirectories(keyFile.getParent());

        logger.info("write key, keyName={}, path={}", keyName, keyFile);
        Files.write(keyFile, content.getBytes(Charsets.UTF_8));

        String osName = System.getProperty("os.name").toLowerCase(Locale.getDefault());
        if (osName.contains("linux") || osName.contains("mac")) {
            HashSet<PosixFilePermission> permissions = new HashSet<>();
            permissions.add(PosixFilePermission.OWNER_READ);
            permissions.add(PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(keyFile, permissions);
        }
    }

    public void deleteKeyPair(String keyName) throws IOException {
        AWS.getEc2().deleteKeyPair(keyName);

        Path keyFile = KeyPair.keyFile(keyName, env);
        if (Files.exists(keyFile)) {
            logger.info("delete key, path={}", keyFile);
            Files.delete(keyFile);
        }
    }
}
