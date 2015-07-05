package core.aws.remote.sns;

import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.sns.Topic;

import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
public class SNSLoader {
    private final Resources resources;
    private final Environment env;

    public SNSLoader(Resources resources, Environment env) {
        this.resources = resources;
        this.env = env;
    }

    public void load() {
        List<com.amazonaws.services.sns.model.Topic> topics = AWS.sns.listTopics();
        for (com.amazonaws.services.sns.model.Topic remoteTopic : topics) {
            String topicARN = remoteTopic.getTopicArn();
            String topicName = topicName(topicARN);

            if (!topicName.startsWith(env.name + "-")) continue;    // ignore topics not match env naming convention
            String resourceId = topicName.substring(env.name.length() + 1);

            Optional<Topic> result = resources.find(Topic.class, resourceId);
            Topic topic = result.isPresent() ? result.get() : resources.add(new Topic(resourceId));
            topic.name = topicName;
            topic.remoteTopicARN = topicARN;
            topic.foundInRemote();
        }
    }

    private String topicName(String topicARN) {
        int index = topicARN.lastIndexOf(':');
        return topicARN.substring(index + 1);
    }
}
