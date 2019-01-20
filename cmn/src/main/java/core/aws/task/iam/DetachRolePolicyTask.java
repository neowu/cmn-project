package core.aws.task.iam;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.iam.Role;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Task;

import java.util.List;

/**
 * @author mort
 */
public class DetachRolePolicyTask extends Task<Role> {
    public final List<String> detachedPolicyARNs;

    public DetachRolePolicyTask(Role resource, List<String> detachedPolicyARNs) {
        super(resource);
        this.detachedPolicyARNs = detachedPolicyARNs;
    }

    @Override
    public void execute(Context context) throws Exception {
        String name = resource.remoteRole.getRoleName();
        AWS.getIam().detachRolePolicies(name, detachedPolicyARNs);
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource)
            .add("policies", detachedPolicyARNs)
            .toString();
    }
}
