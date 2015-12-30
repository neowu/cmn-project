package core.aws.plan.vpc;

import com.amazonaws.services.ec2.model.Route;
import core.aws.plan.Planner;
import core.aws.resource.vpc.RouteTable;
import core.aws.task.vpc.CreateInternetGatewayTask;
import core.aws.task.vpc.CreateNATGatewayTask;
import core.aws.task.vpc.CreateRouteTableTask;
import core.aws.task.vpc.CreateVPCTask;
import core.aws.task.vpc.DeleteInternetGatewayTask;
import core.aws.task.vpc.DeleteNATGatewayTask;
import core.aws.task.vpc.DeleteRouteTableTask;
import core.aws.task.vpc.DeleteVPCTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class RouteTableTaskPlanner extends Planner {
    public RouteTableTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        linkCreateTasks();

        linkDeleteTasks();
    }

    private void linkDeleteTasks() {
        for (DeleteRouteTableTask routeTableTask : all(DeleteRouteTableTask.class)) {
            RouteTable routeTable = routeTableTask.resource;
            find(DeleteVPCTask.class)
                .ifPresent(task -> task.dependsOn(routeTableTask));

            for (final Route route : routeTable.remoteRouteTable.getRoutes()) {
                if (route.getGatewayId() != null) {
                    all(DeleteInternetGatewayTask.class).stream()
                        .filter(task -> task.resource.remoteInternetGatewayId.equals(route.getGatewayId()))
                        .findAny().ifPresent(task -> task.dependsOn(routeTableTask));
                } else if (route.getNatGatewayId() != null) {
                    all(DeleteNATGatewayTask.class).stream()
                        .filter(task -> task.resource.remoteNATGateway.getNatGatewayId().equals(route.getNatGatewayId()))
                        .findAny().ifPresent(task -> task.dependsOn(routeTableTask));
                }
            }
        }
    }

    private void linkCreateTasks() {
        for (final CreateRouteTableTask routeTableTask : all(CreateRouteTableTask.class)) {
            find(CreateVPCTask.class).ifPresent(routeTableTask::dependsOn);

            RouteTable routeTable = routeTableTask.resource;

            if (routeTable.nat != null) {
                find(CreateNATGatewayTask.class)
                    .ifPresent(routeTableTask::dependsOn);
            }

            if (routeTable.internetGateway != null) {
                find(CreateInternetGatewayTask.class)
                    .ifPresent(routeTableTask::dependsOn);
            }
        }
    }
}
