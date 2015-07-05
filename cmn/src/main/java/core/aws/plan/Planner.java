package core.aws.plan;

import core.aws.resource.Resource;
import core.aws.util.Asserts;
import core.aws.workflow.Task;
import core.aws.workflow.Tasks;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static core.aws.util.StreamHelper.instanceOf;
import static core.aws.util.StreamHelper.onlyOne;

/**
 * @author neo
 */
public abstract class Planner {
    protected final Tasks tasks;

    public Planner(Tasks tasks) {
        this.tasks = tasks;
    }

    public <T extends Resource, U extends Task> Optional<U> find(final Class<U> taskClass, final T resource) {
        Asserts.notNull(resource, "resource can not be null");
        return tasks.stream().flatMap(instanceOf(taskClass)).filter(task -> task.resource == resource).reduce(onlyOne());
    }

    public <U extends Task> Optional<U> find(Class<U> taskClass) {
        return tasks.stream().flatMap(instanceOf(taskClass)).reduce(onlyOne());
    }

    public <T extends Task> List<T> all(Class<T> taskClass) {
        return tasks.stream().flatMap(instanceOf(taskClass)).collect(Collectors.toList());
    }

    public abstract void plan();
}
