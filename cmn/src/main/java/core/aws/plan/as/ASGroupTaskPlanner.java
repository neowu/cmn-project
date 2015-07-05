package core.aws.plan.as;

import core.aws.plan.Planner;
import core.aws.resource.as.AutoScalingGroup;
import core.aws.task.as.CreateASGroupTask;
import core.aws.task.as.CreatePolicyTask;
import core.aws.task.as.DeleteASGroupTask;
import core.aws.task.as.DeletePolicyTask;
import core.aws.task.ec2.CreateInstanceProfileTask;
import core.aws.task.ec2.CreateKeyPairTask;
import core.aws.task.ec2.CreateSGTask;
import core.aws.task.ec2.DeleteSGTask;
import core.aws.task.elb.CreateELBTask;
import core.aws.task.elb.DeleteELBTask;
import core.aws.task.vpc.CreateSubnetTask;
import core.aws.task.vpc.DeleteSubnetTask;
import core.aws.workflow.Tasks;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author neo
 */
public class ASGroupTaskPlanner extends Planner {
    public ASGroupTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        linkCreateTasks();

        linkDeleteTasks();

        removeUnnecessaryDeletePolicyTasks();
    }

    private void removeUnnecessaryDeletePolicyTasks() {
        Set<DeletePolicyTask> removedTasks = new HashSet<>();

        for (DeletePolicyTask deletePolicyTask : all(DeletePolicyTask.class)) {
            if (all(DeleteASGroupTask.class).stream().anyMatch(agNameEquals(deletePolicyTask))) {
                removedTasks.add(deletePolicyTask); // delete auto scaling group will remove all policies automatically
            }
        }

        tasks.removeAll(removedTasks);
    }

    private Predicate<DeleteASGroupTask> agNameEquals(DeletePolicyTask deletePolicyTask) {
        return asGroupTask -> deletePolicyTask.resource.remotePolicy.getAutoScalingGroupName()
            .equals(asGroupTask.resource.remoteASGroup.getAutoScalingGroupName());
    }

    private void linkDeleteTasks() {
        for (DeleteASGroupTask asGroupTask : all(DeleteASGroupTask.class)) {
            AutoScalingGroup asGroup = asGroupTask.resource;

            if (asGroup.elb != null) {
                find(DeleteELBTask.class, asGroup.elb)
                    .ifPresent(task -> task.dependsOn(asGroupTask));
            }

            find(DeleteSubnetTask.class, asGroup.subnet)
                .ifPresent(task -> task.dependsOn(asGroupTask));

            find(DeleteSGTask.class, asGroup.launchConfig.securityGroup)
                .ifPresent(task -> task.dependsOn(asGroupTask));
        }
    }

    private void linkCreateTasks() {
        all(CreateASGroupTask.class).forEach(asGroupTask -> {
            AutoScalingGroup asGroup = asGroupTask.resource;

            find(CreateSGTask.class, asGroup.launchConfig.securityGroup)
                .ifPresent(asGroupTask::dependsOn);

            if (asGroup.launchConfig.instanceProfile != null) {
                find(CreateInstanceProfileTask.class, asGroup.launchConfig.instanceProfile)
                    .ifPresent(asGroupTask::dependsOn);
            }

            find(CreateKeyPairTask.class, asGroup.launchConfig.keyPair)
                .ifPresent(asGroupTask::dependsOn);

            if (asGroup.elb != null) {
                find(CreateELBTask.class, asGroup.elb)
                    .ifPresent(asGroupTask::dependsOn);
            }

            find(CreateSubnetTask.class, asGroup.subnet)
                .ifPresent(asGroupTask::dependsOn);
        });

        all(CreatePolicyTask.class).forEach(policyTask ->
            find(CreateASGroupTask.class, policyTask.resource.autoScalingGroup)
                .ifPresent(policyTask::dependsOn));
    }
}
