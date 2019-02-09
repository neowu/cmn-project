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
        for (DeleteRoleTask roleTask : all(DeleteRoleTask.class)) {
            if (roleTask.resource.instanceProfile != null) {
                find(DeleteInstanceProfileTask.class, roleTask.resource.instanceProfile)
                    .ifPresent(roleTask::dependsOn);
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
