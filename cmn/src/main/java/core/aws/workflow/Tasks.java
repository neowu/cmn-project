package core.aws.workflow;

import core.aws.env.Context;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Stream;

/**
 * @author neo
 */
public final class Tasks {
    private final Set<Task> tasks = new HashSet<>();

    public <T extends Task> T add(T task) {
        tasks.add(task);
        return task;
    }

    public int size() {
        return tasks.size();
    }

    public void addAll(Tasks tasks) {
        this.tasks.addAll(tasks.tasks);
    }

    public void removeAll(Set<? extends Task> tasks) {
        this.tasks.removeAll(tasks);
    }

    public void execute(Context context) throws ExecutionException, InterruptedException {
        new Workflow(this).execute(context);
    }

    public Stream<Task> stream() {
        return tasks.stream();
    }
}
