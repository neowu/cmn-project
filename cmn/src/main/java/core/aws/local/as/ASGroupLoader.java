package core.aws.local.as;

import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.local.ec2.EBSBuilder;
import core.aws.resource.Resources;
import core.aws.resource.as.ASGroup;
import core.aws.resource.as.AutoScalingPolicy;
import core.aws.resource.ec2.InstanceProfile;
import core.aws.resource.ec2.KeyPair;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.resource.elb.ELB;
import core.aws.resource.elb.v2.TargetGroup;
import core.aws.resource.vpc.Subnet;
import core.aws.util.Asserts;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


/**
 * @author neo
 */
public class ASGroupLoader implements LocalResourceLoader {
    @Override
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        String imageId = node.requiredString("ami");
        String instanceType = node.requiredString("instance-type");
        String securityGroupId = node.requiredString("security-group");
        String subnetId = node.requiredString("subnet");

        Map<String, Object> capacity = node.mapField("capacity");
        int minSize = (int) Asserts.notNull(capacity.get("min"), "min is required for capacity");
        int maxSize = (int) Asserts.notNull(capacity.get("max"), "max is required for capacity");
        int desiredSize = (int) Asserts.notNull(capacity.get("desired"), "desired is required for capacity");

        Optional<String> elbId = node.getString("elb");
        Optional<String> targetGroupId = node.getString("target-group");
        Optional<String> instanceProfileId = node.getString("instance-profile");

        ASGroup asGroup = resources.add(new ASGroup(node.id));
        asGroup.launchConfig.instanceType = instanceType;
        asGroup.launchConfig.ebs = new EBSBuilder().build(node.mapField("ebs"));
        asGroup.launchConfig.keyPair = resources.find(KeyPair.class, node.id)
            .orElseGet(() -> resources.add(new KeyPair(node.id, env.name + ":" + node.id)));
        asGroup.minSize = minSize;
        asGroup.maxSize = maxSize;
        asGroup.desiredSize = desiredSize;

        resolvers.add(node, () -> {
            asGroup.launchConfig.securityGroup = resources.get(SecurityGroup.class, securityGroupId);
            asGroup.launchConfig.ami = resources.ami(env.region, imageId);

            instanceProfileId.ifPresent(id -> asGroup.launchConfig.instanceProfile = resources.get(InstanceProfile.class, id));

            elbId.ifPresent(id -> asGroup.elb = resources.get(ELB.class, id));
            targetGroupId.ifPresent(id -> asGroup.targetGroup = resources.get(TargetGroup.class, id));

            asGroup.subnet = resources.get(Subnet.class, subnetId);
        });

        List<AutoScalingPolicy> policies = loadPolicies(node);
        for (AutoScalingPolicy policy : policies) {
            resources.add(policy);
            policy.asGroup = asGroup;
        }
    }

    @SuppressWarnings("unchecked")
    private List<AutoScalingPolicy> loadPolicies(ResourceNode node) {
        final List<AutoScalingPolicy> policies = new ArrayList<>();
        Object scale = node.field("scale");
        if (scale instanceof Map) {
            policies.add(scale(node.id, (Map<String, Object>) scale));
        } else if (scale instanceof List) {
            policies.addAll(((List<Map<String, Object>>) scale).stream()
                .map(rule -> scale(node.id, rule))
                .collect(Collectors.toList()));
        }
        return policies;
    }

    public AutoScalingPolicy scale(String resourceId, Map<String, Object> params) {
        String name = (String) Asserts.notNull(params.get("name"), "name is required for scale");
        String cpu = (String) Asserts.notNull(params.get("cpu"), "cpu is required for scale");
        String last = (String) Asserts.notNull(params.get("last"), "last is required for scale");
        String adjustment = (String) Asserts.notNull(params.get("adjustment"), "adjustment is required for scale");
        return createPolicy(resourceId, name, cpu, last, adjustment);
    }

    AutoScalingPolicy createPolicy(String resourceId, String name, String cpu, String last, String adjustment) {
        AutoScalingPolicy policy = new AutoScalingPolicy(resourceId + "-" + name);

        Asserts.isTrue(cpu.matches("[>|<]=\\d+%"), "cpu should be like >=80%");
        if (cpu.startsWith(">=")) {
            policy.comparisonOperator = ComparisonOperator.GreaterThanOrEqualToThreshold;
        } else {
            policy.comparisonOperator = ComparisonOperator.LessThanOrEqualToThreshold;
        }
        policy.cpuUtilizationPercentage = Integer.parseInt(cpu.substring(2, cpu.length() - 1));

        Asserts.isTrue(last.endsWith("min"), "last should be like 3min");
        policy.lastMinutes = Integer.parseInt(last.substring(0, last.length() - 3));

        Asserts.isTrue(adjustment.endsWith("%"), "adjustment should be like 15%");
        policy.adjustmentPercentage = Integer.parseInt(adjustment.substring(0, adjustment.length() - 1));
        return policy;
    }
}
