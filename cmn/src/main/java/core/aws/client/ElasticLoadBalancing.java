package core.aws.client;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancing.model.ApplySecurityGroupsToLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeregisterInstancesFromLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeInstanceHealthResult;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancing.model.Instance;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import com.amazonaws.services.elasticloadbalancing.model.ModifyLoadBalancerAttributesRequest;
import com.amazonaws.services.elasticloadbalancing.model.RegisterInstancesWithLoadBalancerRequest;
import core.aws.util.Runner;
import core.aws.util.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author neo
 */
public class ElasticLoadBalancing {
    public final AmazonElasticLoadBalancing elb;
    private final Logger logger = LoggerFactory.getLogger(ElasticLoadBalancing.class);

    public ElasticLoadBalancing(AWSCredentialsProvider credentials, Regions region) {
        elb = AmazonElasticLoadBalancingClientBuilder.standard().withRegion(region).withCredentials(credentials).build();
    }

    public LoadBalancerDescription createELB(final CreateLoadBalancerRequest request) throws Exception {
        new Runner<>()
            .retryInterval(Duration.ofSeconds(20))
            .maxAttempts(5)
            .retryOn(e -> e instanceof AmazonServiceException)
            .run(() -> {
                logger.info("create elb, request={}", request);
                elb.createLoadBalancer(request);
                return null;
            });

        return describeELB(request.getLoadBalancerName());
    }

    public void modifyELBAttributes(ModifyLoadBalancerAttributesRequest request) {
        logger.info("modify elb attributes, request={}", request);
        elb.modifyLoadBalancerAttributes(request);
    }

    public LoadBalancerDescription describeELB(String elbName) {
        logger.info("describe elb, elbName={}", elbName);
        DescribeLoadBalancersResult result = elb.describeLoadBalancers(new DescribeLoadBalancersRequest()
            .withLoadBalancerNames(elbName));
        return result.getLoadBalancerDescriptions().get(0);
    }

    public List<LoadBalancerDescription> listELBs() {
        logger.info("list all elbs");
        DescribeLoadBalancersResult result = elb.describeLoadBalancers();
        return result.getLoadBalancerDescriptions();
    }

    public void deleteELB(String elbName) {
        logger.info("delete elb, elbName={}", elbName);
        elb.deleteLoadBalancer(new DeleteLoadBalancerRequest(elbName));
    }

    public void updateELBSG(String elbName, String sgId) {
        logger.info("apply security group to elb, elbName={}, sgId={}", elbName, sgId);
        elb.applySecurityGroupsToLoadBalancer(new ApplySecurityGroupsToLoadBalancerRequest()
            .withLoadBalancerName(elbName)
            .withSecurityGroups(sgId));
    }

    public List<InstanceState> describeInstanceHealth(String elbName, List<String> instanceIds) {
        logger.info("describe elb instance health, instanceIds={}", instanceIds);

        List<Instance> instances = instanceIds.stream()
            .map(Instance::new)
            .collect(Collectors.toList());

        DescribeInstanceHealthResult result = elb.describeInstanceHealth(new DescribeInstanceHealthRequest(elbName)
            .withInstances(instances));

        return result.getInstanceStates();
    }

    public void detachInstances(String elbName, List<String> instanceIds) {
        logger.info("detach instances from elb, elb={}, instances={}", elbName, instanceIds);

        List<Instance> instances = instanceIds.stream().map(Instance::new).collect(Collectors.toList());

        elb.deregisterInstancesFromLoadBalancer(new DeregisterInstancesFromLoadBalancerRequest()
            .withLoadBalancerName(elbName)
            .withInstances(instances));
    }

    public void attachInstances(String elbName, List<String> instanceIds, boolean waitUntilInService) throws InterruptedException {
        logger.info("attach instances to elb, elb={}, instances={}", elbName, instanceIds);

        String expectedState = waitUntilInService ? "InService" : "Service";    // if not waitUntilInService, state can be InService or OutOfService

        List<Instance> instances = instanceIds.stream().map(Instance::new).collect(Collectors.toList());

        elb.registerInstancesWithLoadBalancer(new RegisterInstancesWithLoadBalancerRequest()
            .withLoadBalancerName(elbName)
            .withInstances(instances));

        int attempts = 0;
        while (true) {
            attempts++;
            Threads.sleepRoughly(Duration.ofSeconds(15));

            List<InstanceState> states = describeInstanceHealth(elbName, instanceIds);

            for (InstanceState state : states) {
                logger.info("instance elb state {} => {}", state.getInstanceId(), state.getState());
            }

            boolean allAttached = states.stream().allMatch(state -> state.getState().contains(expectedState));
            if (allAttached) {
                logger.info("all instances are attached to elb");
                break;
            } else if (attempts >= 30) {
                throw new Error("failed to wait all instances to be attached to elb, please check aws console for more details");
            } else {
                logger.info("continue to wait, not all new instances are attached");
            }
        }
    }
}
