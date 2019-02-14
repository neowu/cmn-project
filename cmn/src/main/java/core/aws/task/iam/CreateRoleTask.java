package core.aws.task.iam;

import com.amazonaws.services.identitymanagement.model.AddRoleToInstanceProfileRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.iam.Role;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author mort
 */
@Action("create-iam-role")
public class CreateRoleTask extends Task<Role> {
    public CreateRoleTask(Role role) {
        super(role);
    }

    @Override
    public void execute(Context context) throws Exception {
        String roleName = resource.name;
        resource.remoteRole = AWS.getIam().createRole(resource.path, roleName, resource.policy, resource.assumeRolePolicy);
        if (!resource.policyARNs.isEmpty()) {
            AWS.getIam().attachRolePolicies(roleName, resource.policyARNs);
        }
        if (resource.instanceProfile != null) {
            AWS.getIam().iam.addRoleToInstanceProfile(new AddRoleToInstanceProfileRequest()
                .withInstanceProfileName(resource.instanceProfile.name)
                .withRoleName(roleName));
        }

        context.output(String.format("role/%s/path", resource.id), resource.path);
    }
}
