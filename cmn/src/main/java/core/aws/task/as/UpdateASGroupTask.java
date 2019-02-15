package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.CreateOrUpdateTagsRequest;
import com.amazonaws.services.autoscaling.model.DeleteTagsRequest;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.as.ASGroup;
import core.aws.util.Lists;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;

/**
 * @author neo
 */
@Action("update-asg")
public class UpdateASGroupTask extends Task<ASGroup> {
    private final Request request;

    public UpdateASGroupTask(ASGroup asGroup, Request request) {
        super(asGroup);
        this.request = request;
    }

    @Override
    public void execute(Context context) throws Exception {
        if (request.essentialChanged) {
            String asGroupName = resource.remoteASGroup.getAutoScalingGroupName();
            updateASGroup(context, asGroupName);
        }
        if (!request.deletedTags.isEmpty()) {
            AWS.getAs().autoScaling.deleteTags(new DeleteTagsRequest().withTags(request.deletedTags));
        }
        if (!request.addedTags.isEmpty()) {
            AWS.getAs().autoScaling.createOrUpdateTags(new CreateOrUpdateTagsRequest().withTags(request.addedTags));
        }
    }

    private void updateASGroup(Context context, String asGroupName) throws Exception {
        String oldLaunchConfigName = null;
        if (resource.launchConfig.changed()) {
            ASGroupHelper helper = new ASGroupHelper(context.env);
            oldLaunchConfigName = resource.remoteASGroup.getLaunchConfigurationName();
            helper.createLaunchConfig(resource);

            AWS.getAs().updateTag(asGroupName, helper.nameTag(resource));
        }

        AWS.getAs().updateASGroup(new UpdateAutoScalingGroupRequest()
            .withAutoScalingGroupName(asGroupName)
            .withLaunchConfigurationName(resource.launchConfig.remoteLaunchConfig.getLaunchConfigurationName())
            .withTerminationPolicies(ASGroup.TERMINATE_POLICY_OLDEST_INSTANCE)
            .withDesiredCapacity(resource.desiredSize)
            .withMinSize(resource.minSize)
            .withMaxSize(resource.maxSize));

        if (oldLaunchConfigName != null) {
            AWS.getAs().deleteLaunchConfig(oldLaunchConfigName);
        }
    }

    @Override
    public String toString() {
        ToStringHelper helper = new ToStringHelper(this).add(resource);
        if (!request.addedTags.isEmpty()) {
            helper.add("add-tags", request.addedTags);
        }
        if (!request.deletedTags.isEmpty()) {
            helper.add("delete-tags", request.deletedTags);
        }
        return helper.toString();
    }

    public static class Request {
        boolean essentialChanged = false;
        List<Tag> addedTags = Lists.newArrayList();
        List<Tag> deletedTags = Lists.newArrayList();

        public Request essentialChanged(boolean essentialChanged) {
            this.essentialChanged = essentialChanged;
            return this;
        }

        public Request addedTags(List<Tag> tags) {
            this.addedTags = tags;
            return this;
        }

        public Request deletedTags(List<Tag> tags) {
            this.deletedTags = tags;
            return this;
        }
    }
}
