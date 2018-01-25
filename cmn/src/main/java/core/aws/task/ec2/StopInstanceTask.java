package core.aws.task.ec2;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.ec2.Instance;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;

/**
 * @author neo
 */
@Action("stop-instance")
public class StopInstanceTask extends Task<Instance> {
    public StopInstanceTask(Instance instance) {
        super(instance);
    }

    @Override
    public void execute(Context context) throws Exception {
        List<String> instanceIds = resource.runningInstanceIds();

        AWS.getEc2().stopInstances(instanceIds);
        context.output("instance/stopped", instanceIds);
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource)
            .add("remoteInstanceIds", resource.runningInstanceIds())
            .toString();
    }
}
