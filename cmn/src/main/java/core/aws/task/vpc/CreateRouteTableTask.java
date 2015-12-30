package core.aws.task.vpc;

import com.amazonaws.services.ec2.model.CreateRouteRequest;
import com.amazonaws.services.ec2.model.CreateRouteTableRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.vpc.RouteTable;
import core.aws.task.ec2.EC2TagHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author neo
 */
@Action("create-route-table")
public class CreateRouteTableTask extends Task<RouteTable> {
    private final Logger logger = LoggerFactory.getLogger(CreateRouteTableTask.class);

    public CreateRouteTableTask(RouteTable routeTable) {
        super(routeTable);
    }

    @Override
    public void execute(Context context) throws Exception {
        logger.info("create route table, routeTable={}", resource.id);
        resource.remoteRouteTable = AWS.vpc.ec2.createRouteTable(new CreateRouteTableRequest().withVpcId(resource.vpc.remoteVPC.getVpcId())).getRouteTable();

        if (resource.internetGateway != null) {
            AWS.vpc.ec2.createRoute(new CreateRouteRequest()
                .withRouteTableId(resource.remoteRouteTable.getRouteTableId())
                .withGatewayId(resource.internetGateway.remoteInternetGatewayId)
                .withDestinationCidrBlock("0.0.0.0/0"));
        } else {
            AWS.vpc.ec2.createRoute(new CreateRouteRequest()
                .withRouteTableId(resource.remoteRouteTable.getRouteTableId())
                .withNatGatewayId(resource.nat.remoteNATGateway.getNatGatewayId())
                .withDestinationCidrBlock("0.0.0.0/0"));
        }

        EC2TagHelper tagHelper = new EC2TagHelper(context.env);

        AWS.ec2.createTags(new CreateTagsRequest()
            .withResources(resource.remoteRouteTable.getRouteTableId())
            .withTags(tagHelper.env(), tagHelper.name(resource.id), tagHelper.resourceId(resource.id)));
    }
}
