package core.aws.local.iam;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.iam.Role;
import core.aws.util.Files;

import java.util.List;

/**
 * @author mort
 */
public class RoleLoader implements LocalResourceLoader {
    @Override
    @SuppressWarnings("unchecked")
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        final Role role = resources.add(new Role(node.id));
        role.name = env.name + "-" + node.id;
        role.path = node.requiredString("path");
        role.assumeRolePolicyDocument = Files.text(env.envDir.resolve(node.requiredString("assume-role-policy")));
        role.managedPolicyARNs = (List<String>) node.listField("managed-policy-arns");
    }
}
