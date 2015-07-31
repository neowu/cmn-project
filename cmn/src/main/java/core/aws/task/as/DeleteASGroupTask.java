package core.aws.task.as;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.as.ASGroup;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author neo
 */
@Action("del-asg")
public class DeleteASGroupTask extends Task<ASGroup> {
    public DeleteASGroupTask(ASGroup asGroup) {
        super(asGroup);
    }

    @Override
    public void execute(Context context) throws Exception {
        String asGroupName = resource.remoteASGroup.getAutoScalingGroupName();

        AWS.as.deleteAutoScalingGroup(asGroupName);

        String launchConfigurationName = resource.launchConfig.remoteLaunchConfig.getLaunchConfigurationName();
        AWS.as.deleteLaunchConfig(launchConfigurationName);
        context.output("as/" + resource.id, String.format("deletedASGroup=%s, deletedLaunchConfig=%s", asGroupName, launchConfigurationName));

        List<String> instanceIds = resource.remoteASGroup.getInstances()
            .stream()
            .map(com.amazonaws.services.autoscaling.model.Instance::getInstanceId)
            .collect(Collectors.toList());

        if (!instanceIds.isEmpty())
            AWS.ec2.terminateInstances(instanceIds);
    }
}
