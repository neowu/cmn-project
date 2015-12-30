package core.aws.plan.vpc;

import core.aws.plan.Planner;
import core.aws.task.vpc.CreateNATGatewayTask;
import core.aws.task.vpc.CreateSubnetTask;
import core.aws.task.vpc.DeleteNATGatewayTask;
import core.aws.task.vpc.DeleteSubnetTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class NATGatewayTaskPlanner extends Planner {
    public NATGatewayTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        linkCreateTasks();

        linkDeleteTasks();
    }

    private void linkDeleteTasks() {
        for (DeleteNATGatewayTask natTask : all(DeleteNATGatewayTask.class)) {
            all(DeleteSubnetTask.class).stream()
                .filter(subnetTask -> subnetTask.resource.remoteSubnets.stream()
                    .anyMatch(subnet -> natTask.resource.remoteNATGateway.getSubnetId().equals(subnet.getSubnetId())))
                .findAny().ifPresent(task -> task.dependsOn(natTask));
        }
    }

    private void linkCreateTasks() {
        for (CreateNATGatewayTask natTask : all(CreateNATGatewayTask.class)) {
            find(CreateSubnetTask.class, natTask.resource.subnet)
                .ifPresent(natTask::dependsOn);
        }
    }
}
