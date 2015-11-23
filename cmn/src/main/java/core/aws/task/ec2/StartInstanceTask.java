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
@Action("start-instance")
public class StartInstanceTask extends Task<Instance> {
    public StartInstanceTask(Instance instance) {
        super(instance);
    }

    @Override
    public void execute(Context context) throws Exception {
        List<String> instanceIds = resource.stoppedInstanceIds();

        AWS.ec2.startInstances(instanceIds);

        if (resource.elb != null) {
            // ELB uses instance ip to register, after stop/start, ELB will not refresh if instance ip changed, here it has to reattach instance to ELB
            String elbName = resource.elb.remoteELB.getLoadBalancerName();
            AWS.elb.detachInstances(elbName, instanceIds);
            AWS.elb.attachInstances(elbName, instanceIds, false);
        }

        context.output("instance/" + resource.id, "startedInstanceIds" + instanceIds);
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource)
            .add("remoteInstanceIds", resource.stoppedInstanceIds())
            .toString();
    }
}
