package core.aws.task.ec2;

import com.amazonaws.services.identitymanagement.model.DeleteInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.RemoveRoleFromInstanceProfileRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.ec2.InstanceProfile;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author neo
 */
@Action("del-instance-profile")
public class DeleteInstanceProfileTask extends Task<InstanceProfile> {
    private final Logger logger = LoggerFactory.getLogger(DeleteInstanceProfileTask.class);

    public DeleteInstanceProfileTask(InstanceProfile instanceProfile) {
        super(instanceProfile);
    }

    @Override
    public void execute(Context context) throws Exception {
        String name = resource.remoteInstanceProfile.getInstanceProfileName();

        logger.info("delete instance profile and related role and policy, name={}", name);
        if (!resource.remoteInstanceProfile.getRoles().isEmpty()) { // if the associated role doesn't exist anymore, skip to delete (this is not expected state, cmn create role for every instance profile)
            AWS.getIam().iam.removeRoleFromInstanceProfile(new RemoveRoleFromInstanceProfileRequest()
                .withInstanceProfileName(name)
                .withRoleName(name));
        }
        AWS.getIam().iam.deleteInstanceProfile(new DeleteInstanceProfileRequest().withInstanceProfileName(name));
    }
}
