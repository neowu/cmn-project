package core.aws.remote.sqs;

import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.sqs.Queue;

import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
public class SQSLoader {
    private final Resources resources;
    private final Environment env;

    public SQSLoader(Resources resources, Environment env) {
        this.resources = resources;
        this.env = env;
    }

    public void load() {
        List<String> queueURLs = AWS.sqs.listQueueURLs(env.name);  // can't use env.name + '-', sqs has bug if the prefix ends with '-', it won't return any queue
        for (String queueURL : queueURLs) {
            String queueName = queueName(queueURL);

            if (!queueName.startsWith(env.name + "-")) continue;    // ignore queues not match env naming convention
            String resourceId = queueName.substring(env.name.length() + 1);

            Optional<Queue> result = resources.find(Queue.class, resourceId);
            Queue queue = result.isPresent() ? result.get() : resources.add(new Queue(resourceId));
            queue.name = queueName;
            queue.remoteQueueURL = queueURL;
            queue.foundInRemote();
        }
    }

    private String queueName(String queueURL) {
        int index = queueURL.lastIndexOf('/');
        return queueURL.substring(index + 1);
    }
}