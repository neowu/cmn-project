package core.aws.local.elb;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.resource.elb.ELB;
import core.aws.resource.elb.ServerCert;
import core.aws.resource.s3.Bucket;
import core.aws.resource.vpc.Subnet;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
public class ELBLoader implements LocalResourceLoader {
    @Override
    @SuppressWarnings("unchecked")
    public void load(ResourceNode node, final Resources resources, DependencyResolvers resolvers, Environment env) {
        List<String> listenProtocols = new ArrayList<>();
        Object listen = node.field("listen");
        if (listen instanceof String) listenProtocols.add((String) listen);
        else if (listen instanceof List) listenProtocols.addAll((java.util.Collection<? extends String>) listen);

        boolean listenHTTP = listenProtocols.contains("http");
        boolean listenHTTPS = listenProtocols.contains("https");
        Optional<String> serverCertId = node.getString("cert");
        String healthCheckURL = node.requiredString("health-check");
        String securityGroupId = node.requiredString("security-group");
        String subnetId = node.requiredString("subnet");
        Optional<String> scheme = node.getString("scheme");
        Optional<String> accessLogBucketId = node.getString("access-log-s3-bucket");

        final ELB elb = resources.add(new ELB(node.id));
        elb.name = env.name + "-" + node.id;
        elb.listenHTTP = listenHTTP;
        elb.listenHTTPS = listenHTTPS;
        elb.healthCheckURL = healthCheckURL;
        elb.scheme = scheme;

        resolvers.add(node, () -> {
            serverCertId.ifPresent(id -> {
                if (id.startsWith("arn:aws:acm:")) {
                    elb.amazonCertARN = id;
                } else {
                    elb.cert = resources.get(ServerCert.class, id);
                }
            });

            elb.subnet = resources.get(Subnet.class, subnetId);
            elb.securityGroup = resources.get(SecurityGroup.class, securityGroupId);

            accessLogBucketId.ifPresent(bucketId -> elb.accessLogBucket = resources.get(Bucket.class, bucketId));
        });
    }
}
