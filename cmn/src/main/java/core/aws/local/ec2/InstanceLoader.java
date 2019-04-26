package core.aws.local.ec2;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.ec2.Instance;
import core.aws.resource.ec2.InstanceProfile;
import core.aws.resource.ec2.KeyPair;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.resource.elb.ELB;
import core.aws.resource.vpc.Subnet;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * @author neo
 */
public class InstanceLoader implements LocalResourceLoader {
    @Override
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        String imageId = node.requiredString("ami");
        String instanceType = node.requiredString("instance-type");
        OptionalInt count = node.getInt("count");
        String securityGroupId = node.requiredString("security-group");
        String subnetId = node.requiredString("subnet");
        Optional<String> elbId = node.getString("elb");
        Optional<String> instanceProfileId = node.getString("instance-profile");

        Instance instance = resources.add(new Instance(node.id));
        count.ifPresent(value -> instance.count = value);
        instance.instanceType = instanceType;
        instance.ebs = new EBSBuilder().build(node.mapField("ebs"));
        instance.keyPair = resources.find(KeyPair.class, node.id)
            .orElseGet(() -> resources.add(new KeyPair(node.id, env.name + ":" + node.id)));

        resolvers.add(node, () -> {
            instance.securityGroup = resources.get(SecurityGroup.class, securityGroupId);
            instance.ami = resources.ami(env.region, imageId);
            instance.subnet = resources.get(Subnet.class, subnetId);

            elbId.ifPresent(id -> instance.elb = resources.get(ELB.class, id));

            instanceProfileId.ifPresent(id -> instance.instanceProfile = resources.get(InstanceProfile.class, id));
        });
    }
}
