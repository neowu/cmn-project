package core.aws.plan.elb;

import core.aws.plan.Planner;
import core.aws.task.ec2.CreateSGTask;
import core.aws.task.ec2.DeleteSGTask;
import core.aws.task.elb.CreateELBListenerTask;
import core.aws.task.elb.CreateELBTask;
import core.aws.task.elb.CreateServerCertTask;
import core.aws.task.elb.DeleteELBListenerTask;
import core.aws.task.elb.DeleteELBTask;
import core.aws.task.elb.DeleteServerCertTask;
import core.aws.task.elb.UpdateELBSGTask;
import core.aws.task.s3.CreateBucketTask;
import core.aws.task.vpc.CreateSubnetTask;
import core.aws.task.vpc.DeleteSubnetTask;
import core.aws.workflow.Tasks;

import java.util.List;

/**
 * @author neo
 */
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
        for (UpdateELBSGTask updateELBSGTask : all(UpdateELBSGTask.class)) {
            find(CreateSGTask.class, updateELBSGTask.resource.securityGroup)
                .ifPresent(updateELBSGTask::dependsOn);
        }

        for (CreateELBListenerTask listenerTask : all(CreateELBListenerTask.class)) {
            if (listenerTask.resource.cert != null) {
                find(CreateServerCertTask.class, listenerTask.resource.cert)
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
            final List<String> subnets = elbTask.resource.remoteELB.getSubnets();
            if (!subnets.isEmpty()) {
                all(DeleteSubnetTask.class).stream()
                    .filter(task -> task.resource.remoteSubnets.stream().anyMatch(remoteSubnet -> subnets.contains(remoteSubnet.getSubnetId())))
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
            if (elbTask.resource.subnet != null) {
                find(CreateSubnetTask.class, elbTask.resource.subnet)
                    .ifPresent(elbTask::dependsOn);
            }

            if (elbTask.resource.securityGroup != null) {
                find(CreateSGTask.class, elbTask.resource.securityGroup)
                    .ifPresent(elbTask::dependsOn);
            }

            if (elbTask.resource.cert != null) {
                find(CreateServerCertTask.class, elbTask.resource.cert)
                    .ifPresent(elbTask::dependsOn);
            }

            if (elbTask.resource.accessLogBucket != null) {
                find(CreateBucketTask.class, elbTask.resource.accessLogBucket)
                    .ifPresent(elbTask::dependsOn);
            }
        }
    }
}
