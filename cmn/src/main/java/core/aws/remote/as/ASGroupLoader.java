package core.aws.remote.as;

import com.amazonaws.services.autoscaling.model.DescribePoliciesRequest;
import com.amazonaws.services.autoscaling.model.ScalingPolicy;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.as.AutoScalingGroup;
import core.aws.resource.as.AutoScalingPolicy;

import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
public class ASGroupLoader {
    private final Resources resources;
    private final Environment env;

    public ASGroupLoader(Resources resources, Environment env) {
        this.resources = resources;
        this.env = env;
    }

    public void load() {
        List<com.amazonaws.services.autoscaling.model.AutoScalingGroup> asGroups = AWS.as.listASGroups();

        String prefix = env.name + "-";
        for (com.amazonaws.services.autoscaling.model.AutoScalingGroup remoteASGroup : asGroups) {
            String asGroupName = remoteASGroup.getAutoScalingGroupName();
            if (asGroupName.startsWith(prefix)) {
                String asGroupId = asGroupName.substring(prefix.length());
                Optional<AutoScalingGroup> result = resources.find(AutoScalingGroup.class, asGroupId);
                AutoScalingGroup asGroup = result.isPresent() ? result.get() : resources.add(new AutoScalingGroup(asGroupId));
                asGroup.remoteASGroup = remoteASGroup;
                asGroup.launchConfig.remoteLaunchConfig = AWS.as.describeLaunchConfig(remoteASGroup.getLaunchConfigurationName());
                asGroup.foundInRemote();

                List<ScalingPolicy> remotePolicies = AWS.as.autoScaling.describePolicies(new DescribePoliciesRequest().withAutoScalingGroupName(asGroupName)).getScalingPolicies();
                for (ScalingPolicy remotePolicy : remotePolicies) {
                    String policyId = remotePolicy.getPolicyName();
                    Optional<AutoScalingPolicy> policyResult = resources.find(AutoScalingPolicy.class, policyId);
                    AutoScalingPolicy policy = policyResult.isPresent() ? policyResult.get() : resources.add(new AutoScalingPolicy(policyId));
                    policy.remotePolicy = remotePolicy;
                    policy.foundInRemote();
                }
            }
        }
    }
}