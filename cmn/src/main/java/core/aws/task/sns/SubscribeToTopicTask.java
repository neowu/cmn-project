package core.aws.task.sns;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.sns.Topic;
import core.aws.resource.sqs.Queue;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;

/**
 * @author neo
 */
@Action("subscribe-sns")
public class SubscribeToTopicTask extends Task<Topic> {
    public final List<Queue> addedQueueSubscriptions;

    public SubscribeToTopicTask(Topic topic, List<Queue> addedQueueSubscriptions) {
        super(topic);
        this.addedQueueSubscriptions = addedQueueSubscriptions;
    }

    @Override
    public void execute(Context context) throws Exception {
        for (Queue queue : addedQueueSubscriptions) {
            String queueARN = queue.remoteARN();
            AWS.sns.subscribe(resource.remoteTopicARN, queue.remoteQueueURL, queueARN);
        }
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource)
            .add("queues", addedQueueSubscriptions)
            .toString();
    }
}
