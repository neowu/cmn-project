package core.aws.resource.sqs;

import core.aws.resource.Resource;
import core.aws.resource.Resources;
import core.aws.task.sqs.CreateQueueTask;
import core.aws.task.sqs.DeleteQueueTask;
import core.aws.task.sqs.DescribeQueueTask;
import core.aws.util.Asserts;
import core.aws.workflow.Tasks;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * @author neo
 */
public class Queue extends Resource {
    private static final Pattern QUEUE_URL_PATTERN = Pattern.compile("https\\://sqs\\.([\\w-]+)\\.amazonaws\\.com(\\.cn){0,1}/(\\d+)/([\\w-]+)");

    public String name;
    public String remoteQueueURL;

    public Queue(String id) {
        super(id);
    }

    public String remoteARN() {
        if (remoteQueueURL == null) return null;
        Matcher matcher = QUEUE_URL_PATTERN.matcher(remoteQueueURL);
        Asserts.isTrue(matcher.matches(), "queue url does not match the pattern, queueURL={}", remoteQueueURL);
        String region = matcher.group(1);
        String accountId = matcher.group(3);
        String queueName = matcher.group(4);
        return String.format("arn:%s:sqs:%s:%s:%s", region.startsWith("cn-") ? "aws-cn" : "aws", region, accountId, queueName);
    }

    @Override
    public void validate(Resources resources) {
        Asserts.isTrue(name.length() <= 80, "max length of queue name is 80");
        Asserts.isTrue(name.matches("[\\w\\-]+"), "queue name can only contain alphanumeric, '-' and '_'");
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateQueueTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteQueueTask(this));
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeQueueTask(this));
    }
}
