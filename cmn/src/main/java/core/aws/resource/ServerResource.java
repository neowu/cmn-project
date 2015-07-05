package core.aws.resource;

import core.aws.env.Goal;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public interface ServerResource {
    void commandTasks(Tasks tasks);

    void startTasks(Tasks tasks);

    void stopTasks(Tasks tasks);

    void provisionTasks(Tasks tasks);

    void uploadTasks(Tasks tasks);

    void deployTasks(Tasks tasks);

    default void serverTasks(Goal goal, Tasks tasks) {
        switch (goal) {
            case EXEC:
                commandTasks(tasks);
                break;
            case UPLOAD:
                uploadTasks(tasks);
                break;
            case PROVISION:
                provisionTasks(tasks);
                break;
            case START:
                startTasks(tasks);
                break;
            case STOP:
                stopTasks(tasks);
                break;
            case DEPLOY:
                deployTasks(tasks);
                break;
            default:
                throw new IllegalStateException("unsupported goal, goal=" + goal);
        }
    }
}
