package core.aws.local.elb.v2;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.resource.elb.ServerCert;
import core.aws.resource.elb.v2.ELB;
import core.aws.resource.elb.v2.TargetGroup;
import core.aws.resource.vpc.Subnet;
import core.aws.util.Lists;

import java.util.List;
import java.util.Optional;

public class ELBLoader implements LocalResourceLoader {
    @Override
    @SuppressWarnings("unchecked")
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        List<String> listenProtocols = Lists.newArrayList();
        Object listen = node.field("listen");
        if (listen instanceof String) {
            listenProtocols.add((String) listen);
        } else if (listen instanceof List) {
            listenProtocols.addAll((java.util.Collection<? extends String>) listen);
        }

        boolean listenHTTP = listenProtocols.contains("http");
        boolean listenHTTPS = listenProtocols.contains("https");
        Optional<String> serverCertId = node.getString("cert");
        String securityGroupId = node.requiredString("security-group");
        String subnetId = node.requiredString("subnet");
        String targetGroupId = node.requiredString("target-group");
        Optional<String> scheme = node.getString("scheme");

        final ELB elb = resources.add(new ELB(node.id));
        elb.name = env.name + "-" + node.id;
        elb.listenHTTP = listenHTTP;
        elb.listenHTTPS = listenHTTPS;
        elb.scheme = scheme;

        resolvers.add(node, () -> {
            serverCertId.ifPresent(id -> {
                if (id.startsWith("arn:aws:acm:")) {
                    elb.amazonCertARN = id;
                } else {
                    elb.cert = resources.get(ServerCert.class, id);
                }
            });
            elb.subnets = List.of(resources.get(Subnet.class, subnetId));
            elb.securityGroup = resources.get(SecurityGroup.class, securityGroupId);
            elb.targetGroup = resources.get(TargetGroup.class, targetGroupId);
        });
    }
}
