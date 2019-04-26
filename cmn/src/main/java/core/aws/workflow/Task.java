package core.aws.workflow;

import core.aws.env.Context;
import core.aws.env.Param;
import core.aws.resource.Resource;
import core.aws.util.Asserts;
import core.aws.util.ToStringHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * @author neo
 */
public abstract class Task<T extends Resource> implements Callable<Void> {
    public final Set<Task> dependencies = new HashSet<>();
    public final Set<Task> backwardDependencies = new HashSet<>();
    public final T resource;
    private final Logger logger = LoggerFactory.getLogger(Task.class);
    private final Logger messageLogger = LoggerFactory.getLogger("message");
    private Context context;
    private Workflow workflow;
    private volatile TaskState state = TaskState.NEW;

    public Task(T resource) {
        this.resource = resource;
    }

    public void dependsOn(Task<?> dependencyTask) {
        dependencies.add(dependencyTask);
        dependencyTask.backwardDependencies.add(this);
    }

    public void unlink(Task dependencyTask) {
        Asserts.isTrue(dependencies.remove(dependencyTask), "can not find task in dependencies, this={}, dependencyTask={}", this, dependencyTask);
        Asserts.isTrue(dependencyTask.backwardDependencies.remove(this), "can not find task in backward dependencies, this={}, dependencyTask={}", this, dependencyTask);
    }

    private boolean readyToRun() {
        if (state != TaskState.NEW) return false;
        for (Task dependency : dependencies) {
            if (dependency.state != TaskState.DONE) return false;
        }
        return true;
    }

    boolean done() {
        return state == TaskState.DONE;
    }

    void runIfReady() {
        synchronized (this) {
            if (readyToRun()) {
                state = TaskState.SCHEDULED;
                try {
                    workflow.submit(this);
                } catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    public void injectDependencies(Workflow workflow, Context context) {
        //TODO: find better way to fix findbugs issue
        synchronized (this) {
            this.workflow = workflow;
        }
        this.context = context;
    }

    @Override
    public final Void call() {
        try {
            Thread thread = Thread.currentThread();
            thread.setName(action() + ":" + resource.id + ":" + thread.getId());

            if ("true".equals(context.param(Param.DRY_RUN))) {
                messageLogger.info("dry-run: {}\n", this);
            } else {
                logger.info("execute {}", this);
                execute(context);
            }

            state = TaskState.DONE;
            backwardDependencies.forEach(Task::runIfReady);
        } catch (Throwable e) {
            logger.error("failed to execute task, error={}", e.getMessage(), e);
            state = TaskState.FAILED;
        }
        return null;
    }

    private String action() {
        Action action = getClass().getDeclaredAnnotation(Action.class);
        if (action == null) return getClass().getSimpleName();
        return action.value();
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource)
            .toString();
    }

    public abstract void execute(Context context) throws Exception;
}
