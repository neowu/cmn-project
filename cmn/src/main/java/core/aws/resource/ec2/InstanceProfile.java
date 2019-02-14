package core.aws.resource.ec2;

import core.aws.env.Environment;
import core.aws.resource.Resource;
import core.aws.task.ec2.CreateInstanceProfileTask;
import core.aws.task.ec2.DeleteInstanceProfileTask;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class InstanceProfile extends Resource {
    public static String instanceProfilePath(Environment env) {
        return String.format("/%s/", env.name.replaceAll("-", ""));
    }

    public String name;
    public String path;
    public com.amazonaws.services.identitymanagement.model.InstanceProfile remoteInstanceProfile;

    public InstanceProfile(String id) {
        super(id);
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateInstanceProfileTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteInstanceProfileTask(this));
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(id)
            .add(status)
            .toString();
    }
}
