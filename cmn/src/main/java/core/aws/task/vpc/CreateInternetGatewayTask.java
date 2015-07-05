package core.aws.task.vpc;

import com.amazonaws.services.ec2.model.CreateTagsRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.vpc.InternetGateway;
import core.aws.task.ec2.EC2TagHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("create-internet-gateway")
public class CreateInternetGatewayTask extends Task<InternetGateway> {
    public CreateInternetGatewayTask(InternetGateway internetGateway) {
        super(internetGateway);
    }

    @Override
    public void execute(Context context) throws Exception {
        resource.remoteInternetGatewayId = AWS.vpc.createInternetGateway(resource.vpc.remoteVPC.getVpcId());

        EC2TagHelper tagHelper = new EC2TagHelper(context.env);
        AWS.ec2.createTags(new CreateTagsRequest()
            .withResources(resource.remoteInternetGatewayId)
            .withTags(tagHelper.env(), tagHelper.resourceId(resource.id), tagHelper.name(resource.id)));
    }
}
