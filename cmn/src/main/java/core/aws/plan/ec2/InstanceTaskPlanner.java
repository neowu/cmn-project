package core.aws.plan.ec2;

import core.aws.plan.Planner;
import core.aws.resource.ec2.Instance;
import core.aws.task.ec2.CreateInstanceProfileTask;
import core.aws.task.ec2.CreateInstanceTask;
import core.aws.task.ec2.CreateKeyPairTask;
import core.aws.task.ec2.CreateSGTask;
import core.aws.task.ec2.DeleteInstanceTask;
import core.aws.task.ec2.DeleteSGTask;
import core.aws.task.elb.CreateELBTask;
import core.aws.task.vpc.CreateSubnetTask;
import core.aws.task.vpc.DeleteSubnetTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class InstanceTaskPlanner extends Planner {
    public InstanceTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        linkCreateTasks();
        linkDeleteTasks();
    }

    private void linkDeleteTasks() {
        for (final DeleteInstanceTask instanceTask : all(DeleteInstanceTask.class)) {
            String securityGroupId = instanceTask.deletedInstances.get(0).getSecurityGroups().get(0).getGroupId();
            all(DeleteSGTask.class).stream()
                .filter(task -> securityGroupId.equals(task.resource.remoteSecurityGroup.getGroupId()))
                .findAny().ifPresent(task -> task.dependsOn(instanceTask));

            String subnetId = instanceTask.deletedInstances.get(0).getSubnetId();
            all(DeleteSubnetTask.class).stream()
                .filter(task -> task.resource.remoteSubnets.stream().anyMatch(subnet -> subnet.getSubnetId().equals(subnetId)))
                .findAny().ifPresent(task -> task.dependsOn(instanceTask));
        }
    }

    private void linkCreateTasks() {
        for (CreateInstanceTask instanceTask : all(CreateInstanceTask.class)) {
            Instance instance = instanceTask.resource;

            find(CreateSGTask.class, instance.securityGroup)
                .ifPresent(instanceTask::dependsOn);

            find(CreateSubnetTask.class, instance.subnet)
                .ifPresent(instanceTask::dependsOn);

            if (instance.elb != null) {
                find(CreateELBTask.class, instance.elb)
                    .ifPresent(instanceTask::dependsOn);
            }

            if (instance.instanceProfile != null) {
                find(CreateInstanceProfileTask.class, instance.instanceProfile)
                    .ifPresent(instanceTask::dependsOn);
            }

            find(CreateKeyPairTask.class, instance.keyPair)
                .ifPresent(instanceTask::dependsOn);
        }
    }
}
