package core.aws.client;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.autoscaling.AmazonAutoScaling;
import com.amazonaws.services.autoscaling.AmazonAutoScalingClientBuilder;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.CreateOrUpdateTagsRequest;
import com.amazonaws.services.autoscaling.model.DeleteAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.DeleteLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.DeletePolicyRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeLaunchConfigurationsRequest;
import com.amazonaws.services.autoscaling.model.DescribePoliciesRequest;
import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.autoscaling.model.ScalingPolicy;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.autoscaling.model.TerminateInstanceInAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import core.aws.util.Exceptions;
import core.aws.util.Maps;
import core.aws.util.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author neo
 */
public class AutoScaling {
    public final AmazonAutoScaling autoScaling;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public AutoScaling(AWSCredentialsProvider credentials, Regions region) {
        autoScaling = AmazonAutoScalingClientBuilder.standard().withRegion(region).withCredentials(credentials).build();
    }

    public AutoScalingGroup createASGroup(CreateAutoScalingGroupRequest request) {
        logger.info("create auto scaling group, request={}", request);
        autoScaling.createAutoScalingGroup(request);

        DescribeAutoScalingGroupsResult result = autoScaling.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withAutoScalingGroupNames(request.getAutoScalingGroupName()));
        return result.getAutoScalingGroups().get(0);
    }

    public LaunchConfiguration createLaunchConfig(CreateLaunchConfigurationRequest request) throws Exception {
        return new Runner<LaunchConfiguration>()
            .retryInterval(Duration.ofSeconds(5))
            .maxAttempts(3)
            .retryOn(e -> e instanceof AmazonServiceException)
            .run(() -> {
                logger.info("create launch config, request={}", request);
                autoScaling.createLaunchConfiguration(request);
                return describeLaunchConfig(request.getLaunchConfigurationName());
            });
    }

    public List<AutoScalingGroup> listASGroups() {
        logger.info("list all auto scaling groups");
        List<AutoScalingGroup> asGroups = new ArrayList<>();
        String nextToken = null;
        while (true) {
            DescribeAutoScalingGroupsResult result = autoScaling.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest().withNextToken(nextToken));
            asGroups.addAll(result.getAutoScalingGroups());
            nextToken = result.getNextToken();
            if (nextToken == null) break;
        }
        return asGroups;
    }

    public AutoScalingGroup describeASGroup(String asGroupName) {
        logger.info("describe auto scaling group, name={}", asGroupName);
        List<AutoScalingGroup> groups = autoScaling.describeAutoScalingGroups(new DescribeAutoScalingGroupsRequest()
            .withAutoScalingGroupNames(asGroupName))
                                                   .getAutoScalingGroups();
        if (groups.isEmpty()) return null;
        return groups.get(0);
    }

    public Map<String, LaunchConfiguration> describeLaunchConfigs(List<String> launchConfigNames) {
        if (launchConfigNames.isEmpty()) throw new Error("launchConfigNames must not be empty");
        logger.info("describe launch configs, names={}", launchConfigNames);

        Map<String, LaunchConfiguration> results = Maps.newHashMap();

        autoScaling.describeLaunchConfigurations(new DescribeLaunchConfigurationsRequest()
            .withLaunchConfigurationNames(launchConfigNames))
                   .getLaunchConfigurations()
                   .forEach(config -> results.put(config.getLaunchConfigurationName(), config));

        if (results.size() != launchConfigNames.size())
            throw Exceptions.error("some launch config does not exist, foundNames={}", results.keySet());

        return results;
    }

    public LaunchConfiguration describeLaunchConfig(String launchConfigName) {
        logger.info("describe launch config, name={}", launchConfigName);
        return autoScaling.describeLaunchConfigurations(new DescribeLaunchConfigurationsRequest()
            .withLaunchConfigurationNames(launchConfigName))
                          .getLaunchConfigurations().get(0);
    }

    public List<ScalingPolicy> describeScalingPolicies(String asGroupName) {
        logger.info("describe auto scaling policies, asGroupName={}", asGroupName);
        return autoScaling.describePolicies(new DescribePoliciesRequest().withAutoScalingGroupName(asGroupName)).getScalingPolicies();
    }

    public void updateASGroup(UpdateAutoScalingGroupRequest request) throws Exception {
        new Runner<>()
            .retryInterval(Duration.ofSeconds(5))
            .maxAttempts(3)
            .retryOn(e -> e instanceof AmazonServiceException)
            .run(() -> {
                logger.info("update auto scaling group, request={}", request);
                autoScaling.updateAutoScalingGroup(request);
                return null;
            });
    }

    public String createPolicy(PutScalingPolicyRequest request) {
        logger.info("create scaling policy, request={}", request);
        return autoScaling.putScalingPolicy(request).getPolicyARN();
    }

    public void deletePolicy(String asGroupName, String policyName) {
        logger.info("delete scaling policy, asGroup={}, policy={}", asGroupName, policyName);
        autoScaling.deletePolicy(new DeletePolicyRequest()
            .withAutoScalingGroupName(asGroupName)
            .withPolicyName(policyName));
    }

    public void updateTag(String asGroupName, Tag tag) {
        logger.info("update auto scaling group tag, asGroup={}, key={}, value={}", asGroupName, tag.getKey(), tag.getValue());
        tag.withResourceId(asGroupName).withResourceType("auto-scaling-group");
        autoScaling.createOrUpdateTags(new CreateOrUpdateTagsRequest().withTags(tag));
    }

    public void deleteLaunchConfig(String launchConfigName) {
        logger.info("delete launch config, name={}", launchConfigName);
        autoScaling.deleteLaunchConfiguration(new DeleteLaunchConfigurationRequest()
            .withLaunchConfigurationName(launchConfigName));
    }

    public void terminateInstancesInASGroup(List<String> instanceIds, boolean decrementDesiredCapacity) {
        logger.info("terminate instances in auto scaling group, instanceIds={}, decrementDesiredCapacity={}", instanceIds, decrementDesiredCapacity);
        for (String instanceId : instanceIds) {
            try {
                autoScaling.terminateInstanceInAutoScalingGroup(new TerminateInstanceInAutoScalingGroupRequest()
                    .withInstanceId(instanceId)
                    .withShouldDecrementDesiredCapacity(decrementDesiredCapacity));
            } catch (AmazonClientException e) {
                logger.warn("failed to terminate instance in auto scaling group, it could be already terminated by auto scaling group, instanceId={}", instanceId, e);
            }
        }
    }

    public void deleteAutoScalingGroup(String asGroupName) {
        logger.info("delete auto scaling group, name={}", asGroupName);
        autoScaling.deleteAutoScalingGroup(new DeleteAutoScalingGroupRequest()
            .withAutoScalingGroupName(asGroupName)
            .withForceDelete(Boolean.TRUE));
    }
}
