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
@Action("start-asg")
public class StartASGroupTask extends Task<ASGroup> {
    public StartASGroupTask(ASGroup asGroup) {
        super(asGroup);
    }

    @Override
    public void execute(Context context) throws Exception {
        AWS.getAs().updateASGroup(new UpdateAutoScalingGroupRequest()
            .withAutoScalingGroupName(resource.remoteASGroup.getAutoScalingGroupName())
            .withDesiredCapacity(resource.desiredSize)
            .withMinSize(resource.minSize)
            .withMaxSize(resource.maxSize));
    }
}
