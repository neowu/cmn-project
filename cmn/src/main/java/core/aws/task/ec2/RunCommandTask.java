package core.aws.task.ec2;

import core.aws.env.Context;
import core.aws.resource.ec2.Instance;
import core.aws.task.linux.LinuxCommandRunner;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author neo
 */
@Action("exec-instance")
public class RunCommandTask extends Task<Instance> {
    private final Logger logger = LoggerFactory.getLogger(RunCommandTask.class);

    public RunCommandTask(Instance instance) {
        super(instance);
    }

    @Override
    public void execute(Context context) throws Exception {
        logger.info("execute command, instanceId={}", resource.id);

        LinuxCommandRunner runner = new LinuxCommandRunner(context.env, resource.remoteInstances, context);
        runner.run();
    }
}
