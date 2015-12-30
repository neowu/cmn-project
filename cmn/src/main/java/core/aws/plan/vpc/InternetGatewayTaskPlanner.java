package core.aws.plan.vpc;

import core.aws.plan.Planner;
import core.aws.task.as.DeleteASGroupTask;
import core.aws.task.ec2.DeleteInstanceTask;
import core.aws.task.elb.DeleteELBTask;
import core.aws.task.vpc.CreateInternetGatewayTask;
import core.aws.task.vpc.CreateVPCTask;
import core.aws.task.vpc.DeleteInternetGatewayTask;
import core.aws.task.vpc.DeleteNATGatewayTask;
import core.aws.task.vpc.DeleteVPCTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class InternetGatewayTaskPlanner extends Planner {
    public InternetGatewayTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        linkCreateTasks();

        linkDeleteTasks();
    }

    private void linkDeleteTasks() {
        for (DeleteInternetGatewayTask internetGatewayTask : all(DeleteInternetGatewayTask.class)) {
            find(DeleteVPCTask.class)
                .ifPresent(task -> task.dependsOn(internetGatewayTask));

            // to remove internet gateway requires all mapped public ip deleted
            all(DeleteNATGatewayTask.class).forEach(internetGatewayTask::dependsOn);

            // simply wait until all instance terminated, in theory only need to wait all public subnet instances
            all(DeleteInstanceTask.class).forEach(internetGatewayTask::dependsOn);

            all(DeleteASGroupTask.class).forEach(internetGatewayTask::dependsOn);

            all(DeleteELBTask.class).forEach(internetGatewayTask::dependsOn);
        }
    }

    private void linkCreateTasks() {
        find(CreateInternetGatewayTask.class)
            .ifPresent(internetGatewayTask -> find(CreateVPCTask.class)
                .ifPresent(internetGatewayTask::dependsOn));
    }
}
