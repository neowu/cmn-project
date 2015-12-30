package core.aws.resource.vpc;

import core.aws.resource.Resource;
import core.aws.task.vpc.CreateRouteTableTask;
import core.aws.task.vpc.DeleteRouteTableTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class RouteTable extends Resource {
    public static final String PUBLIC_ROUTE_TABLE_RESOURCE_ID = "public";
    public static final String PRIVATE_ROUTE_TABLE_RESOURCE_ID = "private";
    public com.amazonaws.services.ec2.model.RouteTable remoteRouteTable;
    public NATGateway nat;
    public InternetGateway internetGateway;
    public VPC vpc;

    public RouteTable(String id) {
        super(id);
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateRouteTableTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteRouteTableTask(this));
    }
}
