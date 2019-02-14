package core.aws.task.iam;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.iam.Role;
import core.aws.util.Strings;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;

/**
 * @author mort
 */
@Action("del-iam-role")
public class DeleteRoleTask extends Task<Role> {
    public DeleteRoleTask(Role resource) {
        super(resource);
    }

    @Override
    public void execute(Context context) throws Exception {
        String name = resource.remoteRole.getRoleName();
        List<String> detachedPolicyARNs = AWS.getIam().listAttachedRolePolicyARNs(name);
        if (!detachedPolicyARNs.isEmpty()) {
            AWS.getIam().detachRolePolicies(name, detachedPolicyARNs);
        }
        AWS.getIam().deleteRole(name, resource.remoteRole.getPath());

        context.output("role/%s" + resource.id, Strings.format("deletedRole={}:{}", name, resource.remoteRole.getPath()));
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource.getClass().getSimpleName() + "{" + resource.id)
            .add(resource.status)
            .add("path", resource.remoteRole.getPath() + "}")
            .toString();
    }
}
