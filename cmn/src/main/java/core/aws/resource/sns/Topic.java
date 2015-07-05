package core.aws.resource.sns;

import core.aws.client.AWS;
import core.aws.resource.Resource;
import core.aws.resource.Resources;
import core.aws.resource.sqs.Queue;
import core.aws.task.sns.CreateTopicTask;
import core.aws.task.sns.DeleteTopicTask;
import core.aws.task.sns.DescribeTopicTask;
import core.aws.task.sns.SubscribeToTopicTask;
import core.aws.util.Asserts;
import core.aws.util.Lists;
import core.aws.workflow.Tasks;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author neo
 */
public class Topic extends Resource {
    public String name;
    public String remoteTopicARN;
    public List<Queue> subscriptions = Lists.newArrayList();

    public Topic(String id) {
        super(id);
    }

    @Override
    public void validate(Resources resources) {
        Asserts.isTrue(name.length() <= 256, "max length of queue name is 256");
        Asserts.isTrue(name.matches("[\\w\\-]+"), "topic name can only contains alphanumeric, '-' and '_'");
    }

    @Override
    protected void createTasks(Tasks tasks) {
        CreateTopicTask createTask = tasks.add(new CreateTopicTask(this));
        if (!subscriptions.isEmpty()) {
            SubscribeToTopicTask subscribeTask = tasks.add(new SubscribeToTopicTask(this, subscriptions));
            subscribeTask.dependsOn(createTask);
        }
    }

    @Override
    protected void updateTasks(Tasks tasks) {
        List<Queue> addedQueueSubscriptions = addedQueueSubscription();
        if (!addedQueueSubscriptions.isEmpty()) {
            tasks.add(new SubscribeToTopicTask(this, addedQueueSubscriptions));
        }
    }

    // currently only check to be added sqs subscriptions
    private List<Queue> addedQueueSubscription() {
        List<Queue> addedQueueSubscriptions = Lists.newArrayList();
        if (!subscriptions.isEmpty()) {
            List<String> queueARNs = AWS.sns.listSubscribedQueues(remoteTopicARN);
            addedQueueSubscriptions.addAll(subscriptions.stream()
                .filter(subscription -> !queueARNs.contains(subscription.remoteARN()))
                .collect(Collectors.toList()));
        }
        return addedQueueSubscriptions;
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteTopicTask(this));
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeTopicTask(this));
    }
}
