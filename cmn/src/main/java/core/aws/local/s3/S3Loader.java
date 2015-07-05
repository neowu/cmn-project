package core.aws.local.s3;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.s3.Bucket;

/**
 * @author neo
 */
public class S3Loader implements LocalResourceLoader {
    @Override
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        Bucket bucket = new Bucket(node.id);
        bucket.name = env.name + "-" + node.id;
        resources.add(bucket);
    }
}
