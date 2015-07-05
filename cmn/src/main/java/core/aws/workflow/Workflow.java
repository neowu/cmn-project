package core.aws.workflow;

import core.aws.env.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author neo
 */
public class Workflow {
    private final Logger logger = LoggerFactory.getLogger(Workflow.class);

    private final Tasks tasks;
    private final ExecutorService executorService = Executors.newFixedThreadPool(20);
    private final BlockingQueue<Future> futures = new LinkedBlockingQueue<>();

    public Workflow(Tasks tasks) {
        this.tasks = tasks;
    }

    public void execute(Context context) throws InterruptedException, ExecutionException {
        tasks.stream().forEach(task -> task.injectDependencies(this, context));

        tasks.stream().forEach(Task::runIfReady);

        finish();

        checkUnfinishedTasks();
    }

    private void finish() throws ExecutionException, InterruptedException {
        while (!futures.isEmpty()) {
            Future future = futures.take();
            future.get();
        }

        executorService.shutdown();
    }

    void submit(Task<?> task) throws InterruptedException {
        Future<Void> future = executorService.submit(task);
        futures.put(future);
    }

    @SuppressWarnings("unchecked")
    private void checkUnfinishedTasks() {
        List<Task> unfinishedTasks = new ArrayList<>();
        tasks.stream().forEach(task -> {
            if (!task.done()) {
                unfinishedTasks.add(task);
                logger.error("task didn't finish, task={}", task);
                for (Task dependency : (Set<Task>) task.dependencies) {
                    logger.error("task depends on {}", dependency);
                }
            }
        });
        if (!unfinishedTasks.isEmpty())
            throw new IllegalStateException("failed to finish all tasks, unfinishedTasks=" + unfinishedTasks);
    }
}
