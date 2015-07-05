package core.aws.local;

/**
 * @author neo
 */
@FunctionalInterface
public interface DependencyResolver {
    void resolve();
}
