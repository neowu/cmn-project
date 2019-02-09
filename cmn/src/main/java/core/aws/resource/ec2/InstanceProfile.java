package core.aws.resource.ec2;

import core.aws.env.Environment;
import core.aws.resource.Resource;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.task.ec2.CreateInstanceProfileTask;
import core.aws.task.ec2.DeleteInstanceProfileTask;
import core.aws.task.ec2.UpdateInstanceProfileTask;
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
    public String policy;
    public com.amazonaws.services.identitymanagement.model.InstanceProfile remoteInstanceProfile;

    public InstanceProfile(String id) {
        super(id);
    }

    @Override
    public void validate(Resources resources) {
        if (status == ResourceStatus.LOCAL_ONLY || status == ResourceStatus.LOCAL_REMOTE) {
            InstanceProfileHelper helper = new InstanceProfileHelper();
            helper.validatePolicyDocument(policy);
        }
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
    protected void updateTasks(Tasks tasks) {
        InstanceProfileHelper helper = new InstanceProfileHelper();
        if (helper.policyChanged(policy, remoteInstanceProfile)) {
            tasks.add(new UpdateInstanceProfileTask(this));
        }
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(id)
            .add(status)
            .toString();
    }
}
