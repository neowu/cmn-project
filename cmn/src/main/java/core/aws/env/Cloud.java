package core.aws.env;

import core.aws.local.ResourcesLoader;
import core.aws.plan.TaskPlanner;
import core.aws.remote.RemoteResourceLoader;
import core.aws.resource.Resources;
import core.aws.task.TaskBuilder;
import core.aws.task.linux.SSHRunner;
import core.aws.util.Asserts;
import core.aws.workflow.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

/**
 * @author neo
 */
public class Cloud {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ParamValidator validator = new ParamValidator();
    private final ResourcesLoader resourcesLoader = new ResourcesLoader();
    private final RemoteResourceLoader remoteResourceLoader = new RemoteResourceLoader();
    private final Context context;

    public Cloud(Context context) {
        this.context = context;
    }

    @SuppressWarnings("MoveVariableInsideIf")
    public void execute() throws ExecutionException, InterruptedException, IOException {
        validator.validate(context.goal, context.params);

        Path envDir = loadEnvDir(context);
        logger.info("load env, dir={}", envDir);
        context.env = new Environment(envDir);
        Resources resources = resourcesLoader.load(context.env);    // load env and init AWS clients

        Goal goal = context.goal;
        logger.info("goal => {}", goal);
        if (goal == Goal.SSH) {
            ssh();
        } else {
            remoteResourceLoader.load(context.env, resources);
            resources.validate();

            Tasks tasks = new TaskBuilder(goal, resources, context).build();
            if (tasks.size() > 0) {
                new TaskPlanner().plan(tasks);
                tasks.execute(context);
                context.printOutputs();
            } else {
                logger.info("there is no task needs to run");
            }
        }
    }

    private void ssh() throws IOException, InterruptedException {
        // ssh is blocking task
        String resourceId = context.requiredParam(Param.RESOURCE_ID);
        String instanceIndex = context.param(Param.INSTANCE_INDEX);
        String tunnelResourceId = context.param(Param.SSH_TUNNEL_RESOURCE_ID);
        SSHRunner runner = new SSHRunner(context.env, resourceId, instanceIndex == null ? null : Integer.valueOf(instanceIndex), tunnelResourceId);
        runner.run();
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
