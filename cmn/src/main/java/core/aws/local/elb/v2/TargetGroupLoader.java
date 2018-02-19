package core.aws.local.elb.v2;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.elb.v2.TargetGroup;

public class TargetGroupLoader implements LocalResourceLoader {
    private static final String DEFAULT_PROTOCOL = "http";
    private static final int DEFAULT_PORT = 80;

    @Override
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        String healthCheckURL = node.requiredString("health-check");
        TargetGroup targetGroup = resources.add(new TargetGroup(node.id));
        targetGroup.name = env.name + "-" + node.id;
        targetGroup.healthCheckURL = healthCheckURL;
        targetGroup.vpc = resources.vpc;
        targetGroup.protocol = node.getString("protocol").orElse(DEFAULT_PROTOCOL);
        targetGroup.port = node.getInt("port").orElse(DEFAULT_PORT);
    }
}
