package core.aws.task.vpc;

import com.amazonaws.services.ec2.model.CreateTagsRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.env.Environment;
import core.aws.resource.vpc.VPC;
import core.aws.task.ec2.EC2TagHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("create-vpc")
public class CreateVPCTask extends Task<VPC> {
    public CreateVPCTask(VPC vpc) {
        super(vpc);
    }

    @Override
    public void execute(Context context) throws Exception {
        resource.remoteVPC = AWS.vpc.createVPC();

        createTag(context.env);
    }

    private void createTag(Environment env) throws Exception {
        EC2TagHelper tagHelper = new EC2TagHelper(env);

        CreateTagsRequest createTagsRequest = new CreateTagsRequest()
            .withResources(resource.remoteVPC.getVpcId())
            .withTags(tagHelper.env(), tagHelper.resourceId(resource.id), tagHelper.name(resource.id));
        AWS.ec2.createTags(createTagsRequest);
    }
}
