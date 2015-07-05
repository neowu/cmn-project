package core.aws.local.sns;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.sns.Topic;
import core.aws.resource.sqs.Queue;

import java.util.List;

/**
 * @author neo
 */
public class SNSLoader implements LocalResourceLoader {
    @Override
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        Topic topic = resources.add(new Topic(node.id));
        topic.name = env.name + "-" + node.id;

        @SuppressWarnings("unchecked")
        List<String> subscriptions = (List<String>) node.listField("sqs-subscription");
        resolvers.add(node, () -> {
            if (subscriptions != null) {
                for (String subscription : subscriptions) {
                    topic.subscriptions.add(resources.get(Queue.class, subscription));
                }
            }
        });
    }
}