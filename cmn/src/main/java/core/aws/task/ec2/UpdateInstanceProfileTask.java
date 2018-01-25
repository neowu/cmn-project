package core.aws.task.ec2;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.ec2.InstanceProfile;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("update-instance-profile")
public class UpdateInstanceProfileTask extends Task<InstanceProfile> {
    public UpdateInstanceProfileTask(InstanceProfile instanceProfile) {
        super(instanceProfile);
    }

    @Override
    public void execute(Context context) throws Exception {
        String name = resource.remoteInstanceProfile.getInstanceProfileName();
        AWS.getIam().createRolePolicy(name, name, resource.policy);
    }
}
