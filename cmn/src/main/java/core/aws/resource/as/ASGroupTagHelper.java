package core.aws.resource.as;

import com.amazonaws.services.autoscaling.model.Tag;
import core.aws.env.Environment;
import core.aws.task.as.ASGroupHelper;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mort
 */
public class ASGroupTagHelper {
    public static List<Tag> findDeletedTags(ASGroup asGroup) {
        return asGroup.remoteASGroup.getTags().stream()
            .filter(tagDescription -> asGroup.tags.stream().noneMatch(tag -> tag.getKey().equals(tagDescription.getKey()) && tag.getValue().equals(tagDescription.getValue())))
            .map(tagDescription -> new Tag().withKey(tagDescription.getKey()).withValue(tagDescription.getValue()).withResourceType("auto-scaling-group").withResourceId(asGroup.remoteASGroup.getAutoScalingGroupName()).withPropagateAtLaunch(true))
            .collect(Collectors.toList());
    }

    public static List<Tag> findAddedTags(ASGroup asGroup) {
        return asGroup.tags.stream()
            .filter(tag -> asGroup.remoteASGroup.getTags().stream().noneMatch(tagDescription -> tag.getKey().equals(tagDescription.getKey()) && tag.getValue().equals(tagDescription.getValue())))
            .collect(Collectors.toList());
    }

    private final Environment env;

    public ASGroupTagHelper(Environment env) {
        this.env = env;
    }

    public Tag envTag() {
        return new Tag().withKey("cloud-manager:env").withValue(env.name).withPropagateAtLaunch(true);
    }

    public Tag nameTag(ASGroup asGroup) {
        ASGroupHelper helper = new ASGroupHelper(env);
        return helper.nameTag(asGroup);
    }

    public Tag tag(String key, String value, String resourceId) {
        return new Tag().withKey(key).withValue(value).withResourceType("auto-scaling-group").withResourceId(resourceId).withPropagateAtLaunch(true);
    }
}
