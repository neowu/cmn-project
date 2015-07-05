package core.aws.task.ec2;

import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.util.Threads;
import core.aws.workflow.Action;

import java.time.Duration;

/**
 * @author neo
 */
@Action("create-sg")
public class CreateSGTask extends core.aws.workflow.Task<SecurityGroup> {
    public CreateSGTask(SecurityGroup securityGroup) {
        super(securityGroup);
    }

    @Override
    public void execute(Context context) throws Exception {
        EC2TagHelper tags = new EC2TagHelper(context.env);

        String description = context.env.name + ":" + resource.id;
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest(resource.name, description);
        if (resource.vpc != null) request.withVpcId(resource.vpc.remoteVPC.getVpcId());
        resource.remoteSecurityGroup = AWS.ec2.createSecurityGroup(request);

        Threads.sleepRoughly(Duration.ofSeconds(5));    // wait small period of time, for sg to be visible for creating tag

        AWS.ec2.createTags(new CreateTagsRequest()
            .withResources(resource.remoteSecurityGroup.getGroupId())
            .withTags(tags.env(), tags.resourceId(resource.id), tags.name(resource.id)));
    }
}
