package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.Alarm;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.as.AutoScalingPolicy;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author neo
 */
@Action("del-policy")
public class DeletePolicyTask extends Task<AutoScalingPolicy> {
    public DeletePolicyTask(AutoScalingPolicy policy) {
        super(policy);
    }

    @Override
    public void execute(Context context) throws Exception {
        List<String> alarmNames = resource.remotePolicy.getAlarms().stream()
            .map(Alarm::getAlarmName)
            .collect(Collectors.toList());

        AWS.cloudWatch.deleteAlarms(alarmNames);
        AWS.as.deletePolicy(resource.remotePolicy.getPolicyName(), resource.remotePolicy.getAutoScalingGroupName());
    }
}
