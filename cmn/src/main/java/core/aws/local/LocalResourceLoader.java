package core.aws.local;

import core.aws.env.Environment;
import core.aws.resource.Resources;

/**
 * @author neo
 */
public interface LocalResourceLoader {
    void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env);
}
