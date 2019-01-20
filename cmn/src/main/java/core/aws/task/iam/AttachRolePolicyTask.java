package core.aws.task.iam;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.iam.Role;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;

/**
 * @author mort
 */
@Action("attach-role-policy")
public class AttachRolePolicyTask extends Task<Role> {
    public final List<String> attachedPolicies;

    public AttachRolePolicyTask(Role resource, List<String> attachedPolicies) {
        super(resource);
        this.attachedPolicies = attachedPolicies;
    }

    @Override
    public void execute(Context context) throws Exception {
        String name = resource.remoteRole.getRoleName();
        AWS.getIam().attachRolePolicies(name, attachedPolicies);
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource)
            .add("policies", attachedPolicies)
            .toString();
    }
}
