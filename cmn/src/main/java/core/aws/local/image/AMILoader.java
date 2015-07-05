package core.aws.local.image;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.image.Image;

import java.util.Optional;

/**
 * @author neo
 */
public class AMILoader implements LocalResourceLoader {
    @Override
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        String baseAMI = node.requiredString("base-ami");
        String playbook = node.requiredString("playbook");
        Optional<String> packageDir = node.getString("package-dir");

        Image image = resources.add(new Image(node.id));

        packageDir.ifPresent(dir -> image.packageDir = Optional.of(env.envDir.resolve(dir)));

        image.playbook = env.envDir.resolve(playbook);
        image.namePrefix = env.name + "-" + node.id;

        resolvers.add(node, () -> image.baseAMI = resources.ami(env.region, baseAMI));
    }
}
