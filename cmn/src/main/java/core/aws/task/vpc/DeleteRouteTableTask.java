package core.aws.task.vpc;

import com.amazonaws.services.ec2.model.DeleteRouteTableRequest;
import com.amazonaws.services.ec2.model.DisassociateRouteTableRequest;
import com.amazonaws.services.ec2.model.RouteTableAssociation;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.vpc.RouteTable;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author neo
 */
@Action("del-route-table")
public class DeleteRouteTableTask extends Task<RouteTable> {
    private final Logger logger = LoggerFactory.getLogger(DeleteRouteTableTask.class);

    public DeleteRouteTableTask(RouteTable routeTable) {
        super(routeTable);
    }

    @Override
    public void execute(Context context) throws Exception {
        logger.info("delete route table, routeTableId={}", resource.id);
        for (RouteTableAssociation association : resource.remoteRouteTable.getAssociations()) {
            AWS.vpc.ec2.disassociateRouteTable(new DisassociateRouteTableRequest().withAssociationId(association.getRouteTableAssociationId()));
        }
        AWS.vpc.ec2.deleteRouteTable(new DeleteRouteTableRequest().withRouteTableId(resource.remoteRouteTable.getRouteTableId()));
    }
}
