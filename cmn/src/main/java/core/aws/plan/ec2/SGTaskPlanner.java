package core.aws.plan.ec2;

import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.UserIdGroupPair;
import core.aws.plan.Planner;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.task.ec2.CreateSGRuleTask;
import core.aws.task.ec2.CreateSGTask;
import core.aws.task.ec2.DeleteSGRuleTask;
import core.aws.task.ec2.DeleteSGTask;
import core.aws.task.vpc.CreateVPCTask;
import core.aws.task.vpc.DeleteVPCTask;
import core.aws.util.StreamHelper;
import core.aws.workflow.Task;
import core.aws.workflow.Tasks;

import java.util.HashSet;
import java.util.Set;

/**
 * @author neo
 */
public class SGTaskPlanner extends Planner {
    public SGTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        linkCreateTasks();

        linkDeleteTasks();

        removeUnnecessaryDeleteRuleTasks();
    }

    private void removeUnnecessaryDeleteRuleTasks() {
        Set<DeleteSGRuleTask> removedTasks = new HashSet<>();

        for (DeleteSGRuleTask deleteRuleTask : all(DeleteSGRuleTask.class)) {
            Set<Task> backwardDependencies = deleteRuleTask.backwardDependencies;
            if (backwardDependencies.size() == 1) {
                DeleteSGTask deleteTask = (DeleteSGTask) backwardDependencies.stream().reduce(StreamHelper.onlyOne()).get();
                // if only delete group task depends on delete its own rule task, we don't need delete rule task
                if (deleteRuleTask.resource == deleteTask.resource) {
                    deleteTask.unlink(deleteRuleTask);
                    removedTasks.add(deleteRuleTask);
                }
            }
        }

        tasks.removeAll(removedTasks);
    }

    private void linkCreateTasks() {
        all(CreateSGTask.class).stream()
            .filter(sgTask -> sgTask.resource.vpc != null)
            .forEach(sgTask -> find(CreateVPCTask.class).ifPresent(sgTask::dependsOn));

        for (CreateSGRuleTask ruleTask : all(CreateSGRuleTask.class)) {
            ruleTask.addedIngressRules.forEach((protocol, sources) -> sources.forEach(source -> {
                final SecurityGroup sourceSG = source.securityGroup;
                if (sourceSG != null) {
                    find(CreateSGTask.class, sourceSG)
                        .ifPresent(ruleTask::dependsOn);
                }
            }));
        }
    }

    private void linkDeleteTasks() {
        all(DeleteSGRuleTask.class)
            .forEach(this::linkDeleteRuleTask);

        for (DeleteSGTask sgTask : all(DeleteSGTask.class)) {
            if (sgTask.resource.remoteSecurityGroup.getVpcId() != null) {
                find(DeleteVPCTask.class).ifPresent(task -> task.dependsOn(sgTask));
            }
        }
    }

    private void linkDeleteRuleTask(DeleteSGRuleTask ruleTask) {
        for (IpPermission rule : ruleTask.deletedIngressRules) {
            for (UserIdGroupPair userIdGroup : rule.getUserIdGroupPairs()) {
                final String sourceSGId = userIdGroup.getGroupId();

                all(DeleteSGTask.class).stream()
                    .filter(task -> sourceSGId.equals(task.resource.remoteSecurityGroup.getGroupId()))
                    .findAny().ifPresent(task -> task.dependsOn(ruleTask));
            }
        }
    }
}
