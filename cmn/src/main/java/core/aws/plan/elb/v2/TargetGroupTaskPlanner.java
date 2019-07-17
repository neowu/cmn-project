package core.aws.plan.elb.v2;

import core.aws.plan.Planner;
import core.aws.task.elb.v2.CreateTargetGroupTask;
import core.aws.task.elb.v2.DeleteELBListenerTask;
import core.aws.task.elb.v2.DeleteELBTask;
import core.aws.task.elb.v2.DeleteTargetGroupTask;
import core.aws.task.vpc.CreateVPCTask;
import core.aws.workflow.Tasks;

public class TargetGroupTaskPlanner extends Planner {
    public TargetGroupTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        linkCreateTasks();
        linkDeleteTasks();
    }

    private void linkDeleteTasks() {
        for (DeleteTargetGroupTask task : all(DeleteTargetGroupTask.class)) {
            all(DeleteELBTask.class).stream()
                .filter(elbTask -> task.resource.remoteTG.getLoadBalancerArns().contains(elbTask.resource.remoteELB.getLoadBalancerArn()))
                .forEach(task::dependsOn);
            all(DeleteELBListenerTask.class).stream()
                .filter(listenerTask -> listenerTask.resource.targetGroup.name.equals(task.resource.name))
                .forEach(task::dependsOn);
        }
    }

    private void linkCreateTasks() {
        for (CreateTargetGroupTask task : all(CreateTargetGroupTask.class)) {
            all(CreateVPCTask.class).forEach(task::dependsOn);
        }
    }
}
