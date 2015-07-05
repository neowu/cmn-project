package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.Instance;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.as.AutoScalingGroup;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author neo
 */
@Action("desc-asg")
public class DescribeASGroupTask extends Task<AutoScalingGroup> {
    public DescribeASGroupTask(AutoScalingGroup asGroup) {
        super(asGroup);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "autoScaling/" + resource.id;
        context.output(key, String.format("status=%s, ami=%s", resource.status, resource.launchConfig.ami.id()));
        com.amazonaws.services.autoscaling.model.AutoScalingGroup remoteASGroup = resource.remoteASGroup;
        if (remoteASGroup != null) {
            context.output(key, String.format("name=%s, min=%s, desired=%s, max=%s, imageId=%s",
                remoteASGroup.getAutoScalingGroupName(),
                remoteASGroup.getMinSize(),
                remoteASGroup.getDesiredCapacity(),
                remoteASGroup.getMaxSize(),
                resource.launchConfig.remoteLaunchConfig.getImageId()));

            List<String> instanceIds = remoteASGroup.getInstances().stream().map(Instance::getInstanceId).collect(Collectors.toList());

            if (!instanceIds.isEmpty()) {
                for (com.amazonaws.services.ec2.model.Instance remoteInstance : AWS.ec2.describeInstances(instanceIds)) {
                    context.output(key, String.format("instanceId=%s, state=%s, privateDNS=%s, privateIP=%s",
                        remoteInstance.getInstanceId(),
                        remoteInstance.getState().getName(),
                        remoteInstance.getPrivateDnsName(),
                        remoteInstance.getPrivateIpAddress()));
                }
            }
        }
    }
}
