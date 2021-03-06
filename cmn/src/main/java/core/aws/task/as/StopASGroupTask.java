package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.as.ASGroup;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("stop-asg")
public class StopASGroupTask extends Task<ASGroup> {
    public StopASGroupTask(ASGroup asGroup) {
        super(asGroup);
    }

    @Override
    public void execute(Context context) throws Exception {
        AWS.getAs().updateASGroup(new UpdateAutoScalingGroupRequest()
            .withAutoScalingGroupName(resource.remoteASGroup.getAutoScalingGroupName())
            .withDesiredCapacity(0)
            .withMinSize(0)
            .withMaxSize(0));
    }
}
