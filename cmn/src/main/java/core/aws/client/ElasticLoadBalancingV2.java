package core.aws.client;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancing;
import com.amazonaws.services.elasticloadbalancingv2.AmazonElasticLoadBalancingClientBuilder;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DeleteLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeListenersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeLoadBalancersResult;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTargetGroupsResult;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.SetSecurityGroupsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup;
import core.aws.util.Lists;
import core.aws.util.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * @author gabo
 */
public class ElasticLoadBalancingV2 {
    private static final int PAGE_SIZE = 50;
    private final Logger logger = LoggerFactory.getLogger(ElasticLoadBalancingV2.class);
    public final AmazonElasticLoadBalancing elb;

    ElasticLoadBalancingV2(AWSCredentialsProvider credentials, Regions region) {
        elb = AmazonElasticLoadBalancingClientBuilder.standard().withRegion(region).withCredentials(credentials).build();
    }

    public LoadBalancer createELB(final CreateLoadBalancerRequest request) throws Exception {
        new Runner<>()
            .retryInterval(Duration.ofSeconds(20))
            .maxAttempts(5)
            .retryOn(e -> e instanceof AmazonServiceException)
            .run(() -> {
                logger.info("create elb, type={}, request={}", request.getType(), request);
                elb.createLoadBalancer(request);
                return null;
            });

        return describeELB(request.getName());
    }

    public LoadBalancer describeELB(String elbName) {
        logger.info("describe elb, elbName={}", elbName);
        DescribeLoadBalancersResult result = elb.describeLoadBalancers(new DescribeLoadBalancersRequest()
            .withNames(elbName));
        return result.getLoadBalancers().get(0);
    }

    public List<LoadBalancer> listELBs() {
        logger.info("list all elbs - V2");
        List<LoadBalancer> result = Lists.newArrayList();
        DescribeLoadBalancersResult loadBalancers = elb.describeLoadBalancers(new DescribeLoadBalancersRequest()
            .withPageSize(PAGE_SIZE));
        String nextMarker;
        while (true) {
            result.addAll(loadBalancers.getLoadBalancers());
            nextMarker = loadBalancers.getNextMarker();
            if (nextMarker != null && nextMarker.length() > 0) {
                loadBalancers = elb.describeLoadBalancers(new DescribeLoadBalancersRequest()
                    .withMarker(nextMarker));
            } else {
                return result;
            }
        }
    }

    public List<TargetGroup> listTGs() {
        logger.info("list all target groups");
        List<TargetGroup> result = Lists.newArrayList();
        DescribeTargetGroupsResult targetGroups = elb.describeTargetGroups(new DescribeTargetGroupsRequest()
            .withPageSize(PAGE_SIZE));
        String nextMarker;
        while (true) {
            result.addAll(targetGroups.getTargetGroups());
            nextMarker = targetGroups.getNextMarker();
            if (nextMarker != null && nextMarker.length() > 0) {
                targetGroups = elb.describeTargetGroups(new DescribeTargetGroupsRequest()
                    .withMarker(nextMarker));
            } else {
                return result;
            }
        }
    }

    public void createListener(final CreateListenerRequest request) throws Exception {
        new Runner<>()
            .retryInterval(Duration.ofSeconds(20))
            .maxAttempts(5)
            .retryOn(e -> e instanceof AmazonServiceException)
            .run(() -> {
                logger.info("create listener, request={}", request);
                elb.createListener(request);
                return null;
            });
    }

    public void deleteELB(String elbARN) {
        logger.info("delete elb, elbARN={}", elbARN);
        elb.deleteLoadBalancer(new DeleteLoadBalancerRequest().withLoadBalancerArn(elbARN));
    }

    public void updateELBSG(String elbARN, String sgId) {
        logger.info("apply security group to elb, elbARN={}, sgId={}", elbARN, sgId);
        elb.setSecurityGroups(new SetSecurityGroupsRequest()
            .withSecurityGroups(sgId)
            .withLoadBalancerArn(elbARN));
    }

    public List<Listener> listeners(String elbARN) {
        logger.info("list listeners, elbARN-{}", elbARN);
        return elb.describeListeners(new DescribeListenersRequest()
            .withLoadBalancerArn(elbARN)).getListeners();
    }
}
