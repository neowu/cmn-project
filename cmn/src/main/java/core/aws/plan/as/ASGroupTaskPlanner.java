package core.aws.plan.as;

import core.aws.plan.Planner;
import core.aws.resource.as.ASGroup;
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
import core.aws.task.elb.v2.CreateTargetGroupTask;
import core.aws.task.vpc.CreateSubnetTask;
import core.aws.task.vpc.DeleteSubnetTask;
import core.aws.workflow.Tasks;

import java.util.HashSet;
import java.util.List;
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
            ASGroup asGroup = asGroupTask.resource;

            List<String> elbNames = asGroup.remoteASGroup.getLoadBalancerNames();
            if (!elbNames.isEmpty()) {
                all(DeleteELBTask.class).stream()
                    .filter(task -> elbNames.contains(task.resource.remoteELB.getLoadBalancerName()))
                    .findAny().ifPresent(task -> task.dependsOn(asGroupTask));
            }

            String subnetIds = asGroup.remoteASGroup.getVPCZoneIdentifier();
            all(DeleteSubnetTask.class).stream()
                .filter(task -> subnetIds.contains(task.resource.firstRemoteSubnet().getSubnetId()))
                .findAny().ifPresent(task -> task.dependsOn(asGroupTask));

            List<String> sgIds = asGroup.launchConfig.remoteLaunchConfig.getSecurityGroups();
            all(DeleteSGTask.class).stream()
                .filter(task -> sgIds.contains(task.resource.remoteSecurityGroup.getGroupId()))
                .findAny().ifPresent(task -> task.dependsOn(asGroupTask));
        }
    }

    private void linkCreateTasks() {
        all(CreateASGroupTask.class).forEach(asGroupTask -> {
            ASGroup asGroup = asGroupTask.resource;

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

            if (asGroup.targetGroup != null) {
                find(CreateTargetGroupTask.class, asGroup.targetGroup)
                    .ifPresent(asGroupTask::dependsOn);
            }

            find(CreateSubnetTask.class, asGroup.subnet)
                .ifPresent(asGroupTask::dependsOn);
        });

        all(CreatePolicyTask.class).forEach(policyTask ->
            find(CreateASGroupTask.class, policyTask.resource.asGroup)
                .ifPresent(policyTask::dependsOn));
    }
}
