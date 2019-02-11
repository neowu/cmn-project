package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.CreateOrUpdateTagsRequest;
import com.amazonaws.services.autoscaling.model.DeleteTagsRequest;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.autoscaling.model.TagDescription;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.as.ASGroup;
import core.aws.util.Lists;
import core.aws.util.Maps;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        String asGroupName = resource.remoteASGroup.getAutoScalingGroupName();

        if (request.essentialChanged) {
            updateASGroup(context, asGroupName);
        }
        if (!request.detachedTags.isEmpty()) {
            List<Tag> tags = request.detachedTags.stream().map(tagDescription -> autoScalingGroupTag(asGroupName, tagDescription.getKey(), tagDescription.getValue())).collect(Collectors.toList());
            AWS.getAs().autoScaling.deleteTags(new DeleteTagsRequest().withTags(tags));
        }
        if (!request.attachedTags.isEmpty()) {
            List<Tag> tags = Lists.newArrayList();
            request.attachedTags.forEach((key, value) -> tags.add(autoScalingGroupTag(asGroupName, key, value)));
            AWS.getAs().autoScaling.createOrUpdateTags(new CreateOrUpdateTagsRequest().withTags(tags));
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

    private Tag autoScalingGroupTag(String asGroupName, String key, String value) {
        return new Tag().withKey(key).withValue(value).withResourceType("auto-scaling-group").withResourceId(asGroupName).withPropagateAtLaunch(true);
    }

    public static class Request {
        boolean essentialChanged = false;
        Map<String, String> attachedTags = Maps.newHashMap();
        List<TagDescription> detachedTags = Lists.newArrayList();

        public Request essentialChanged(boolean essentialChanged) {
            this.essentialChanged = essentialChanged;
            return this;
        }

        public Request attachedTags(Map<String, String> tags) {
            this.attachedTags = tags;
            return this;
        }

        public Request detachedTags(List<TagDescription> tags) {
            this.detachedTags = tags;
            return this;
        }
    }
}
