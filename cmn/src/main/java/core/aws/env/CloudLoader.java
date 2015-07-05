package core.aws.env;

import core.aws.local.ResourcesLoader;
import core.aws.remote.RemoteResourceLoader;
import core.aws.resource.Resources;
import core.aws.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author neo
 */
public class CloudLoader {
    private final Logger logger = LoggerFactory.getLogger(CloudLoader.class);

    private final ParamValidator validator = new ParamValidator();
    private final ResourcesLoader resourcesLoader = new ResourcesLoader();
    private final RemoteResourceLoader remoteResourceLoader = new RemoteResourceLoader();

    public Cloud load(Context context) throws ScriptException, IOException {
        validator.validate(context.goal, context.params);

        Path envDir = loadEnvDir(context);
        logger.info("load env, dir={}", envDir);

        Environment env = new Environment(envDir);
        context.env = env;

        Resources resources = resourcesLoader.load(env);
        remoteResourceLoader.load(env, resources);

        resources.validate();
        return new Cloud(env, resources);
    }

    private Path loadEnvDir(Context context) throws IOException {
        Path envDir;
        String envParam = context.param(Param.ENV_PATH);
        if (envParam != null) {
            envDir = Paths.get(envParam);
        } else {
            envDir = Paths.get(System.getProperty("user.dir"));
        }
        Asserts.isTrue(Files.isDirectory(envDir), "envDir is not directory, path={}", envDir);
        Asserts.isTrue(Files.list(envDir).anyMatch(file -> file.getFileName().toString().endsWith(".yml")), "can not find any yml files under env folder, path={}", envDir);
        return envDir;
    }
}
