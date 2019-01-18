package core.aws.task.iam;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.iam.Role;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author mort
 */
@Action("update-iam-role-policy")
public class UpdateRolePolicyTask extends Task<Role> {
    public UpdateRolePolicyTask(Role resource) {
        super(resource);
    }

    @Override
    public void execute(Context context) throws Exception {
        String name = resource.remoteRole.getRoleName();
        AWS.getIam().detachRolePolicies(name);
        if (!resource.managedPolicyARNs.isEmpty()) {
            AWS.getIam().attachRolePolicies(name, resource.managedPolicyARNs);
        }
    }
}
