package core.aws.task.ec2;

import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteTagsRequest;
import com.amazonaws.services.ec2.model.Tag;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;

/**
 * @author neo
 */
@Action("update-sg-tag")
public class UpdateSGTagTask extends Task<SecurityGroup> {
    private final List<Tag> addedTags;
    private final List<Tag> deletedTags;

    public UpdateSGTagTask(SecurityGroup securityGroup, List<Tag> addedTags, List<Tag> deletedTags) {
        super(securityGroup);
        this.addedTags = addedTags;
        this.deletedTags = deletedTags;
    }

    @Override
    public void execute(Context context) throws Exception {
        if (!deletedTags.isEmpty()) {
            AWS.getEc2().deleteTags(new DeleteTagsRequest()
                .withResources(resource.remoteSecurityGroup.getGroupId())
                .withTags(deletedTags));
        }

        if (!addedTags.isEmpty()) {
            AWS.getEc2().createTags(new CreateTagsRequest()
                .withResources(resource.remoteSecurityGroup.getGroupId())
                .withTags(addedTags));
        }

    }

    @Override
    public String toString() {
        ToStringHelper helper = new ToStringHelper(this).add(resource);
        if (!addedTags.isEmpty()) {
            helper.add("add-tags", addedTags);
        }
        if (!deletedTags.isEmpty()) {
            helper.add("delete-tags", deletedTags);
        }
        return helper.toString();
    }
}
