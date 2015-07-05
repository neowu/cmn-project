package core.aws.env;

import com.amazonaws.regions.Regions;
import core.aws.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * @author neo
 */
public class Environment {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public final Path envDir;
    public Regions region = Regions.US_EAST_1;
    public String name;
    public final List<Path> ansibleRolePaths = new ArrayList<>();
    public String bakeSubnetId;

    public Environment(Path envDir) throws IOException {
        this.envDir = envDir;

        name = envDir.getFileName().toString();

        File ansibleDir = new File(Asserts.notEmpty(System.getProperty("cmn.ansible"), "check start script, make sure -Dcmn.ansible is set")).getCanonicalFile();
        Asserts.isTrue(ansibleDir.isDirectory(), "ansible path must be directory, path={}", ansibleDir);
        logger.info("built-in ansible path => {}", ansibleDir);
        ansibleRolePaths.add(ansibleDir.toPath());
    }

    public void region(String region) {
        this.region = Regions.fromName(region);
    }

    public void name(String name) {
        logger.info("environment set name => {}", name);
        this.name = name;
    }

    public void customAnsiblePath(Path customAnsiblePath) {
        logger.info("custom ansible path => {}", customAnsiblePath);
        ansibleRolePaths.add(customAnsiblePath);
    }
}
