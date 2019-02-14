package core.aws.plan.ec2;

import core.aws.plan.Planner;
import core.aws.task.ec2.CreateInstanceProfileTask;
import core.aws.task.ec2.DeleteInstanceProfileTask;
import core.aws.task.iam.CreateRoleTask;
import core.aws.task.iam.DeleteRoleTask;
import core.aws.workflow.Tasks;

/**
 * @author mort
 */
public class RoleTaskPlanner extends Planner {
    public RoleTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        linkCreateTasks();
        linkDeleteTasks();
    }

    private void linkDeleteTasks() {
        for (DeleteInstanceProfileTask deleteInstanceProfileTask : all(DeleteInstanceProfileTask.class)) {
            if (!deleteInstanceProfileTask.resource.remoteInstanceProfile.getRoles().isEmpty()) {
                String remoteRoleId = deleteInstanceProfileTask.resource.remoteInstanceProfile.getRoles().get(0).getRoleId();
                all(DeleteRoleTask.class).stream()
                    .filter(task -> task.resource.remoteRole.getRoleId().equals(remoteRoleId))
                    .findAny().ifPresent(task -> task.dependsOn(deleteInstanceProfileTask));
            }
        }
    }

    private void linkCreateTasks() {
        for (CreateRoleTask createRoleTask : all(CreateRoleTask.class)) {
            if (createRoleTask.resource.instanceProfile != null) {
                find(CreateInstanceProfileTask.class, createRoleTask.resource.instanceProfile)
                    .ifPresent(createRoleTask::dependsOn);
            }
        }
    }
}
