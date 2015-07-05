package core.aws.task.ec2;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.ec2.Instance;
import core.aws.util.Threads;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author neo
 */
@Action("del-instance")
public class DeleteInstanceTask extends Task<Instance> {
    private final Logger logger = LoggerFactory.getLogger(DeleteInstanceTask.class);

    public final List<com.amazonaws.services.ec2.model.Instance> deletedInstances;

    public DeleteInstanceTask(Instance instance, List<com.amazonaws.services.ec2.model.Instance> deletedInstances) {
        super(instance);
        this.deletedInstances = deletedInstances;
    }

    @Override
    public void execute(Context context) throws Exception {
        List<String> instanceIds = remoteInstanceIds();

        if (resource.elb != null && resource.elb.remoteELB != null) {
            String elbName = resource.elb.remoteELB.getLoadBalancerName();
            AWS.elb.detachInstances(elbName, instanceIds);

            // with ELB draining, wait a bit to finish all current request
            logger.info("sleep a bit to wait all existing requests to finish if any");
            Threads.sleepRoughly(Duration.ofSeconds(5));
        }

        AWS.ec2.terminateInstances(instanceIds);
        context.output("instance/" + resource.id, "deletedInstanceIds=" + instanceIds);
    }

    private List<String> remoteInstanceIds() {
        return deletedInstances.stream()
            .map(com.amazonaws.services.ec2.model.Instance::getInstanceId)
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource.id)
            .add("remoteInstanceIds", remoteInstanceIds())
            .toString();
    }
}
