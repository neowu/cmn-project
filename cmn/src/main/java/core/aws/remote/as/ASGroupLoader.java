package core.aws.remote.as;

import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.ScalingPolicy;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.as.ASGroup;
import core.aws.resource.as.AutoScalingPolicy;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        String prefix = env.name + "-";

        // find all AS group with prefix
        List<AutoScalingGroup> asGroups = AWS.getAs().listASGroups().stream()
                                                .filter(group -> group.getAutoScalingGroupName().startsWith(prefix))
                                                .collect(Collectors.toList());

        if (asGroups.isEmpty()) return;

        // load remote launch config in one request to maximize the speed
        List<String> launchConfigNames = asGroups.stream().map(AutoScalingGroup::getLaunchConfigurationName).collect(Collectors.toList());
        Map<String, LaunchConfiguration> configs = AWS.getAs().describeLaunchConfigs(launchConfigNames);

        for (AutoScalingGroup remoteASGroup : asGroups) {
            String asGroupName = remoteASGroup.getAutoScalingGroupName();
            String asGroupId = asGroupName.substring(prefix.length());
            ASGroup asGroup = resources.find(ASGroup.class, asGroupId)
                                       .orElseGet(() -> resources.add(new ASGroup(asGroupId)));
            asGroup.remoteASGroup = remoteASGroup;
            asGroup.launchConfig.remoteLaunchConfig = configs.get(remoteASGroup.getLaunchConfigurationName());
            asGroup.foundInRemote();

            List<ScalingPolicy> remotePolicies = AWS.getAs().describeScalingPolicies(asGroupName);
            for (ScalingPolicy remotePolicy : remotePolicies) {
                String policyId = remotePolicy.getPolicyName();
                AutoScalingPolicy policy = resources.find(AutoScalingPolicy.class, policyId)
                                                    .orElseGet(() -> resources.add(new AutoScalingPolicy(policyId)));
                policy.remotePolicy = remotePolicy;
                policy.foundInRemote();
            }
        }
    }
}