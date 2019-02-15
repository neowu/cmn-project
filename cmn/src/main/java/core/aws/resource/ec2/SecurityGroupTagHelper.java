package core.aws.resource.ec2;

import com.amazonaws.services.ec2.model.Tag;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author mort
 */
public class SecurityGroupTagHelper {
    private final List<Tag> tags;
    private final List<Tag> remoteTags;

    public SecurityGroupTagHelper(List<Tag> tags, List<Tag> remoteTags) {
        this.tags = tags;
        this.remoteTags = remoteTags;
    }

    List<Tag> findDeletedTags() {
        return remoteTags.stream().filter(remoteTag -> tags.stream().noneMatch(tag -> tag.getKey().equals(remoteTag.getKey()) && tag.getValue().equals(remoteTag.getValue()))).collect(Collectors.toList());
    }

    List<Tag> findAddedTags() {
        return tags.stream().filter(tag -> remoteTags.stream().noneMatch(remoteTag -> remoteTag.getKey().equals(tag.getKey()) && remoteTag.getValue().equals(tag.getValue()))).collect(Collectors.toList());
    }
}
