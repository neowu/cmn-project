package core.aws.local.sqs;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.sqs.Queue;

/**
 * @author neo
 */
public class SQSLoader implements LocalResourceLoader {
    @Override
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        Queue queue = resources.add(new Queue(node.id));
        queue.name = env.name + "-" + node.id;
    }
}