package core.aws.local.ec2;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.ec2.InstanceProfile;
import core.aws.util.Files;

/**
 * @author neo
 */
public class InstanceProfileLoader implements LocalResourceLoader {
    @Override
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        final InstanceProfile instanceProfile = resources.add(new InstanceProfile(node.id));
        instanceProfile.name = env.name + "-" + node.id;
        instanceProfile.policy = Files.text(env.envDir.resolve(node.requiredString("policy")));
    }
}
