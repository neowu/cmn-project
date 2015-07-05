package core.aws.plan.vpc;

import com.amazonaws.services.ec2.model.Subnet;
import core.aws.plan.Planner;
import core.aws.task.vpc.CreateRouteTableTask;
import core.aws.task.vpc.CreateSubnetTask;
import core.aws.task.vpc.CreateVPCTask;
import core.aws.task.vpc.DeleteRouteTableTask;
import core.aws.task.vpc.DeleteSubnetTask;
import core.aws.task.vpc.DeleteVPCTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class SubnetTaskPlanner extends Planner {
    public SubnetTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        linkCreateTasks();

        linkDeleteTasks();
    }

    private void linkDeleteTasks() {
        for (DeleteSubnetTask subnetTask : all(DeleteSubnetTask.class)) {
            find(DeleteVPCTask.class)
                .ifPresent(task -> task.dependsOn(subnetTask));

            for (Subnet remoteSubnet : subnetTask.resource.remoteSubnets) {
                linkDeleteSubnetTasks(subnetTask, remoteSubnet);
            }
        }
    }

    private void linkDeleteSubnetTasks(DeleteSubnetTask subnetTask, Subnet remoteSubnet) {
        for (DeleteRouteTableTask routeTableTask : all(DeleteRouteTableTask.class)) {
            routeTableTask.resource.remoteRouteTable.getAssociations().stream()
                .filter(association -> association.getSubnetId().equals(remoteSubnet.getSubnetId()))
                .forEach(association -> subnetTask.dependsOn(routeTableTask));
        }
    }

    private void linkCreateTasks() {
        for (final CreateSubnetTask subnetTask : all(CreateSubnetTask.class)) {
            find(CreateVPCTask.class)
                .ifPresent(subnetTask::dependsOn);

            find(CreateRouteTableTask.class, subnetTask.resource.routeTable)
                .ifPresent(subnetTask::dependsOn);
        }
    }
}
