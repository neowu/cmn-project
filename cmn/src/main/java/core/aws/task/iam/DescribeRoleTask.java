package core.aws.task.iam;

import core.aws.env.Context;
import core.aws.resource.iam.Role;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author mort
 */
@Action("desc-iam-role")
public class DescribeRoleTask extends Task<Role> {
    public DescribeRoleTask(Role resource) {
        super(resource);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "role/" + resource.id;
        context.output(key, String.format("status=%s, path=%s, attachedPolicies=%s",
            resource.status,
            resource.path,
            resource.policyARNs));

        com.amazonaws.services.identitymanagement.model.Role remoteRole = resource.remoteRole;
        if (remoteRole != null) {
            context.output(key, String.format("roleId=%s, name=%s",
                remoteRole.getRoleId(),
                remoteRole.getRoleName()));
        }
    }
}
