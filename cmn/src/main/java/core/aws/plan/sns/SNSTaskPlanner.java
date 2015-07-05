package core.aws.plan.sns;

import core.aws.plan.Planner;
import core.aws.resource.sqs.Queue;
import core.aws.task.sns.SubscribeToTopicTask;
import core.aws.task.sqs.CreateQueueTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class SNSTaskPlanner extends Planner {
    public SNSTaskPlanner(Tasks tasks) {
        super(tasks);
    }

    @Override
    public void plan() {
        for (SubscribeToTopicTask subscribeTask : all(SubscribeToTopicTask.class)) {
            for (Queue queue : subscribeTask.addedQueueSubscriptions) {
                find(CreateQueueTask.class, queue)
                    .ifPresent(subscribeTask::dependsOn);
            }
        }
    }
}
