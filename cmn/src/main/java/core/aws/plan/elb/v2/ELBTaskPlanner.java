package core.aws.plan.elb.v2;

import core.aws.plan.Planner;
import core.aws.task.ec2.CreateSGTask;
import core.aws.task.ec2.DeleteSGTask;
import core.aws.task.elb.CreateServerCertTask;
import core.aws.task.elb.DeleteServerCertTask;
import core.aws.task.elb.v2.CreateELBListenerTask;
import core.aws.task.elb.v2.CreateELBTask;
import core.aws.task.elb.v2.CreateTargetGroupTask;
import core.aws.task.elb.v2.DeleteELBListenerTask;
import core.aws.task.elb.v2.DeleteELBTask;
import core.aws.task.elb.v2.UpdateELBSGTask;
import core.aws.task.vpc.CreateSubnetTask;
import core.aws.task.vpc.DeleteSubnetTask;
import core.aws.workflow.Tasks;
import org.apache.commons.compress.utils.Sets;

import java.util.List;
import java.util.Set;

public class ELBTaskPlanner extends Planner {
    public ELBTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        linkCreateTasks();
        linkDeleteTasks();
        linkUpdateTasks();
    }

    private void linkUpdateTasks() {
        for (UpdateELBSGTask elbsgTask : all(UpdateELBSGTask.class)) {
            find(CreateSGTask.class, elbsgTask.resource.securityGroup)
                .ifPresent(elbsgTask::dependsOn);
        }

        for (CreateELBListenerTask listenerTask : all(CreateELBListenerTask.class)) {
            if (listenerTask.resource.cert != null) {
                find(CreateELBListenerTask.class, listenerTask.resource.cert)
                    .ifPresent(listenerTask::dependsOn);
            }
        }

        for (DeleteELBListenerTask listenerTask : all(DeleteELBListenerTask.class)) {
            if (listenerTask.resource.cert != null) {
                find(DeleteServerCertTask.class, listenerTask.resource.cert)
                    .ifPresent(task -> task.dependsOn(listenerTask));
            }
        }
    }

    private void linkDeleteTasks() {
        for (DeleteELBTask elbTask : all(DeleteELBTask.class)) {
            final Set<String> subnets = Sets.newHashSet();
            elbTask.resource.subnets.forEach(subnet -> subnet.remoteSubnets.forEach(remoteSubnet -> subnets.add(remoteSubnet.getSubnetId())));
            if (!subnets.isEmpty()) {
                all(DeleteSubnetTask.class).stream()
                    .filter(subnetTask -> subnetTask.resource.remoteSubnets.stream().anyMatch(remoteSubnet -> subnets.contains(remoteSubnet.getSubnetId())))
                    .findAny().ifPresent(task -> task.dependsOn(elbTask));
            }

            final List<String> sgIds = elbTask.resource.remoteELB.getSecurityGroups();
            if (!sgIds.isEmpty()) {
                all(DeleteSGTask.class).stream()
                    .filter(sgTask -> sgIds.contains(sgTask.resource.remoteSecurityGroup.getGroupId()))
                    .findAny().ifPresent(task -> task.dependsOn(elbTask));
            }

            if (elbTask.resource.cert != null) {
                find(DeleteServerCertTask.class, elbTask.resource.cert)
                    .ifPresent(task -> task.dependsOn(elbTask));
            }
        }
    }

    private void linkCreateTasks() {
        for (CreateELBTask elbTask : all(CreateELBTask.class)) {
            if (elbTask.resource.subnets != null) {
                elbTask.resource.subnets.forEach(subnet -> find(CreateSubnetTask.class, subnet).ifPresent(elbTask::dependsOn));
            }

            if (elbTask.resource.securityGroup != null) {
                find(CreateSGTask.class, elbTask.resource.securityGroup)
                    .ifPresent(elbTask::dependsOn);
            }

            if (elbTask.resource.cert != null) {
                find(CreateServerCertTask.class, elbTask.resource.cert)
                    .ifPresent(elbTask::dependsOn);
            }

            if (elbTask.resource.targetGroup != null) {
                find(CreateTargetGroupTask.class, elbTask.resource.targetGroup)
                    .ifPresent(elbTask::dependsOn);
            }
        }
    }
}
