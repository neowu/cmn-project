package core.aws.env;

import core.aws.plan.TaskPlanner;
import core.aws.resource.Resources;
import core.aws.task.TaskBuilder;
import core.aws.task.linux.SSHRunner;
import core.aws.workflow.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

/**
 * @author neo
 */
public class Cloud {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public final Environment env;
    public final Resources resources;

    public Cloud(Environment env, Resources resources) {
        this.env = env;
        this.resources = resources;
    }

    public void execute(Context context) throws ExecutionException, InterruptedException, IOException {
        Goal goal = context.goal;
        logger.info("goal => {}", goal);
        if (goal == Goal.SSH) {
            // ssh is blocking task, so handle specifically
            SSHRunner runner = new SSHRunner(resources, env, context);
            runner.run();
        } else {
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
}
