package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.as.AutoScalingGroup;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("stop-asg")
public class StopASGroupTask extends Task<AutoScalingGroup> {
    public StopASGroupTask(AutoScalingGroup asGroup) {
        super(asGroup);
    }

    @Override
    public void execute(Context context) throws Exception {
        AWS.as.updateASGroup(new UpdateAutoScalingGroupRequest()
            .withAutoScalingGroupName(resource.remoteASGroup.getAutoScalingGroupName())
            .withDesiredCapacity(0)
            .withMinSize(0)
            .withMaxSize(0));
    }
}
