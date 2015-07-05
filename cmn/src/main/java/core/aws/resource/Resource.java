package core.aws.resource;

import core.aws.env.Goal;
import core.aws.util.Asserts;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class Resource {
    public final String id;
    public ResourceStatus status;

    public Resource(String id) {
        this.id = id;
    }

    public void foundInLocal() {
        Asserts.isNull(status, "local status is always checked first");
        status = ResourceStatus.LOCAL_ONLY;
    }

    public void foundInRemote() {
        if (status == null) status = ResourceStatus.REMOTE_ONLY;
        if (status == ResourceStatus.LOCAL_ONLY) status = ResourceStatus.LOCAL_REMOTE;
    }

    public final void tasks(Goal goal, Tasks tasks) {
        switch (goal) {
            case SYNC:
                buildSyncTasks(tasks);
                break;
            case DEL:
                buildDeleteTasks(tasks);
                break;
            case DESC:
                describeTasks(tasks);
                break;
            default:
                throw new IllegalStateException("unsupported goal, goal=" + goal);
        }
    }

    private void buildDeleteTasks(Tasks tasks) {
        if (status == ResourceStatus.LOCAL_REMOTE || status == ResourceStatus.REMOTE_ONLY) {
            deleteTasks(tasks);
        }
    }

    private void buildSyncTasks(Tasks tasks) {
        if (status == ResourceStatus.LOCAL_ONLY) {
            createTasks(tasks);
        } else if (status == ResourceStatus.LOCAL_REMOTE) {
            updateTasks(tasks);
        } else if (status == ResourceStatus.REMOTE_ONLY) {
            deleteTasks(tasks);
        }
    }

    protected void describeTasks(Tasks tasks) {
    }

    protected void createTasks(Tasks tasks) {
    }

    protected void updateTasks(Tasks tasks) {
    }

    protected void deleteTasks(Tasks tasks) {
    }

    public void validate(Resources resources) {
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(id)
            .add(status)
            .toString();
    }
}
