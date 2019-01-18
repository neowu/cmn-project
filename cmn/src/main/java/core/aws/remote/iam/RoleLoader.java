package core.aws.remote.iam;

import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.iam.Role;

import java.util.List;

/**
 * @author mort
 */
public class RoleLoader {
    private final Resources resources;
    private final Environment env;

    public RoleLoader(Resources resources, Environment env) {
        this.resources = resources;
        this.env = env;
    }

    public void load() {
        // list all the roles here
        List<com.amazonaws.services.identitymanagement.model.Role> roles = AWS.getIam().listRoles("/");

        String prefix = env.name + "-";
        for (com.amazonaws.services.identitymanagement.model.Role remoteRole : roles) {
            String name = remoteRole.getRoleName();
            if (!name.startsWith(prefix)) continue; // ignore roles not matching naming convention
            String resourceId = name.substring(env.name.length() + 1);

            Role role = resources.find(Role.class, resourceId).orElseGet(() -> resources.add(new Role(resourceId)));
            role.remoteRole = remoteRole;
            role.remoteManagedPolicyARNs = AWS.getIam().listRolePolicyARNs(name);
            role.foundInRemote();
        }
    }
}
