package core.aws.task.iam;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.iam.Role;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author mort
 */
@Action("update-iam-role")
public class UpdateRoleTask extends Task<Role> {

    public UpdateRoleTask(Role resource) {
        super(resource);
    }

    @Override
    public void execute(Context context) throws Exception {
        AWS.getIam().deleteRole(resource.remoteRole.getRoleName(), resource.remoteRole.getPath());
        AWS.getIam().createRole(resource.path, resource.name, resource.assumeRolePolicyDocument, resource.managedPolicyARNs);
    }
}
