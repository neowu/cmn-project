package core.aws.task.iam;

import com.amazonaws.services.identitymanagement.model.AddRoleToInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.RemoveRoleFromInstanceProfileRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.iam.Role;
import core.aws.util.Lists;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;

/**
 * @author mort
 */
@Action("update-iam-role")
public class UpdateRoleTask extends Task<Role> {
    private final Request request;

    public UpdateRoleTask(Role resource, Request request) {
        super(resource);
        this.request = request;
    }

    @Override
    public void execute(Context context) throws Exception {
        String name = resource.remoteRole.getRoleName();
        if (request.essentialChanged) {
            recreate(name);
            return;
        }
        if (request.policyChanged) {
            AWS.getIam().createRolePolicy(name, name, resource.policy);
        }
        if (!request.attachedPolicyARNs.isEmpty()) {
            AWS.getIam().attachRolePolicies(name, request.attachedPolicyARNs);
        }
        if (!request.detachedPolicyARNs.isEmpty()) {
            AWS.getIam().detachRolePolicies(name, request.detachedPolicyARNs);
        }
    }

    private void recreate(String name) {
        // aws role not support updating path and assume role policy doc, so we delete the role first and re-create it with new data
        List<String> detachedPolicyARNs = AWS.getIam().listAttachedRolePolicyARNs(name);
        if (!detachedPolicyARNs.isEmpty()) {
            AWS.getIam().detachRolePolicies(name, detachedPolicyARNs);
        }

        if (resource.instanceProfile != null) {
            AWS.getIam().iam.removeRoleFromInstanceProfile(new RemoveRoleFromInstanceProfileRequest()
                .withInstanceProfileName(resource.instanceProfile.name)
                .withRoleName(name));
        }

        AWS.getIam().deleteRole(name, resource.remoteRole.getPath());

        resource.remoteRole = AWS.getIam().createRole(resource.path, resource.name, resource.policy, resource.assumeRolePolicy);

        if (!resource.policyARNs.isEmpty()) {
            AWS.getIam().attachRolePolicies(name, resource.policyARNs);
        }

        if (resource.instanceProfile != null) {
            AWS.getIam().iam.addRoleToInstanceProfile(new AddRoleToInstanceProfileRequest()
                .withInstanceProfileName(resource.instanceProfile.name)
                .withRoleName(name));
        }
    }

    public static class Request {
        boolean essentialChanged = false;
        boolean policyChanged = false;
        List<String> attachedPolicyARNs = Lists.newArrayList();
        List<String> detachedPolicyARNs = Lists.newArrayList();

        public Request essentialChanged(boolean essentialChanged) {
            this.essentialChanged = essentialChanged;
            return this;
        }

        public Request policyChanged(boolean policyChanged) {
            this.policyChanged = policyChanged;
            return this;
        }

        public Request attachedPolicyARNs(List<String> attachedPolicyARNs) {
            this.attachedPolicyARNs = attachedPolicyARNs;
            return this;
        }

        public Request detachedPolicyARNs(List<String> detachedPolicyARNs) {
            this.detachedPolicyARNs = detachedPolicyARNs;
            return this;
        }
    }
}
