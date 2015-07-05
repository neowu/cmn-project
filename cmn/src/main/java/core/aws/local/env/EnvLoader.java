package core.aws.local.env;

import core.aws.env.Environment;
import core.aws.local.ResourceNode;

/**
 * @author neo
 */
public class EnvLoader {
    public void load(ResourceNode node, Environment env) {
        String envName = node.id;
        env.name(envName);

        node.getString("region").ifPresent(env::region);

        node.getString("custom-ansible").ifPresent(customAnsible -> env.customAnsiblePath(env.envDir.resolve(customAnsible)));

        node.getString("bake-subnet-id").ifPresent(bakeSubnetId -> env.bakeSubnetId = bakeSubnetId);

        //TODO: better way to validate unused field
    }
}
