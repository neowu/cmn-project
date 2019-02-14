package core.aws.local.ec2;

import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.ec2.InstanceProfile;
import core.aws.resource.iam.Role;
import core.aws.util.Files;

import java.util.List;

/**
 * @author neo
 */
public class InstanceProfileLoader implements LocalResourceLoader {
    @Override
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        final InstanceProfile instanceProfile = resources.add(new InstanceProfile(node.id));
        instanceProfile.name = env.name + "-" + node.id;
        instanceProfile.path = node.getString("path").orElse(InstanceProfile.instanceProfilePath(env));
        Role role = loadRole(node, resources, env);
        role.instanceProfile = instanceProfile;
    }

    @SuppressWarnings("unchecked")
    private Role loadRole(ResourceNode node, Resources resources, Environment env) {
        Role role = resources.add(new Role(node.id));
        role.name = env.name + "-" + node.id;
        role.path = node.getString("path").orElse(InstanceProfile.instanceProfilePath(env));
        role.policy = Files.text(env.envDir.resolve(node.requiredString("policy")));
        role.assumeRolePolicy = AWS.getIam().assumeEC2RolePolicyDocument();
        if (node.listField("managed-policy-arns") != null) {
            role.policyARNs = (List<String>) node.listField("managed-policy-arns");
        }

        return role;
    }
}
