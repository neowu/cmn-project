package core.aws.plan.vpc;

import core.aws.plan.Planner;
import core.aws.resource.vpc.NAT;
import core.aws.task.ec2.CreateKeyPairTask;
import core.aws.task.vpc.CreateNATTask;
import core.aws.task.vpc.CreateSubnetTask;
import core.aws.task.vpc.DeleteNATTask;
import core.aws.task.vpc.DeleteSubnetTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class NATTaskPlanner extends Planner {
    public NATTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        linkCreateTasks();

        linkDeleteTasks();
    }

    private void linkDeleteTasks() {
        for (DeleteNATTask natTask : all(DeleteNATTask.class)) {
            final NAT nat = natTask.resource;
            all(DeleteSubnetTask.class).stream()
                .filter(subnetTask -> subnetTask.resource.remoteSubnets.stream()
                    .anyMatch(subnet -> nat.remoteInstance.getSubnetId().equals(subnet.getSubnetId())))
                .findAny().ifPresent(task -> task.dependsOn(natTask));
        }
    }

    private void linkCreateTasks() {
        for (CreateNATTask natTask : all(CreateNATTask.class)) {
            final NAT nat = natTask.resource;

            find(CreateSubnetTask.class, nat.publicSubnet)
                .ifPresent(natTask::dependsOn);

            find(CreateKeyPairTask.class, nat.keyPair)
                .ifPresent(natTask::dependsOn);
        }
    }
}
