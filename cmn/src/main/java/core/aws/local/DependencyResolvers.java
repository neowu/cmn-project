package core.aws.local;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author neo
 */
public class DependencyResolvers {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<ResourceNodeDependencyResolver> resolvers = new ArrayList<>();

    public void add(ResourceNode node, DependencyResolver resolver) {
        resolvers.add(new ResourceNodeDependencyResolver(node, resolver));
    }

    public void resolve() {
        for (ResourceNodeDependencyResolver resolver : resolvers) {
            try {
                resolver.resolver.resolve();
            } catch (Exception e) {
                logger.error("failed to resolve dependency, node=\n{}", resolver.node.yml);
                throw e;
            }
        }
    }

    static class ResourceNodeDependencyResolver {
        final ResourceNode node;
        final DependencyResolver resolver;

        ResourceNodeDependencyResolver(ResourceNode node, DependencyResolver resolver) {
            this.node = node;
            this.resolver = resolver;
        }
    }
}
