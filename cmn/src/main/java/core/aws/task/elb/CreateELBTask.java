package core.aws.task.elb;

import com.amazonaws.regions.Regions;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.elasticloadbalancing.model.AccessLog;
import com.amazonaws.services.elasticloadbalancing.model.ConfigureHealthCheckRequest;
import com.amazonaws.services.elasticloadbalancing.model.ConnectionDraining;
import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancing.model.CrossZoneLoadBalancing;
import com.amazonaws.services.elasticloadbalancing.model.HealthCheck;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerAttributes;
import com.amazonaws.services.elasticloadbalancing.model.ModifyLoadBalancerAttributesRequest;
import com.amazonaws.services.elasticloadbalancing.model.Tag;
import com.amazonaws.services.s3.model.SetBucketPolicyRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.ELB;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author neo
 */
@Action("create-elb")
public class CreateELBTask extends Task<ELB> {
    public CreateELBTask(ELB elb) {
        super(elb);
    }

    @Override
    public void execute(Context context) throws Exception {
        String elbName = context.env.name + "-" + resource.id;
        CreateLoadBalancerRequest request = new CreateLoadBalancerRequest()
            .withLoadBalancerName(elbName)
            .withScheme(resource.scheme.orElse(null))
            .withTags(new Tag().withKey("cloud-manager:env").withValue(context.env.name));

        if (resource.subnet != null) {
            request.withSecurityGroups(resource.securityGroup.remoteSecurityGroup.getGroupId())
                .withSubnets(resource.subnet.remoteSubnets.stream().map(Subnet::getSubnetId).collect(Collectors.toList()));
        } else {
            List<String> zones = AWS.ec2.availabilityZones();
            request.withAvailabilityZones(zones.get(0));
        }

        if (resource.listenHTTP) {
            request.getListeners().add(new Listener("HTTP", 80, 80));
        }

        if (resource.listenHTTPS) {
            request.getListeners().add(new Listener()
                .withProtocol("HTTPS")
                .withLoadBalancerPort(443)
                .withInstanceProtocol("HTTP")
                .withInstancePort(80)
                .withSSLCertificateId(resource.cert.remoteCert.getServerCertificateMetadata().getArn()));
        }

        resource.remoteELB = AWS.elb.createELB(request);

        configureELB(elbName, context.env.region);

        configureHealthCheck(elbName);

        context.output(String.format("elb/%s/DNS", resource.id), resource.remoteELB.getDNSName());
    }

    private void configureELB(String elbName, Regions region) {
        LoadBalancerAttributes attributes = new LoadBalancerAttributes()
            .withConnectionDraining(new ConnectionDraining().withEnabled(true).withTimeout(30));

        // enable cross zone load balance for multi-az
        if (resource.subnet != null && resource.subnet.cidrs.size() > 1) {
            attributes.setCrossZoneLoadBalancing(new CrossZoneLoadBalancing().withEnabled(true));
        }

        if (resource.accessLogBucket != null) {
            configureAccessLog(attributes, region);
        }

        AWS.elb.modifyELBAttributes(new ModifyLoadBalancerAttributesRequest()
            .withLoadBalancerName(elbName)
            .withLoadBalancerAttributes(attributes));
    }

    private void configureAccessLog(LoadBalancerAttributes attributes, Regions region) {
        ELBAccessLogBucketPolicyBuilder builder = new ELBAccessLogBucketPolicyBuilder();
        String bucketName = resource.accessLogBucket.remoteBucket.getName();
        AWS.s3.s3.setBucketPolicy(new SetBucketPolicyRequest(bucketName, builder.policyText(region, bucketName)));

        attributes.withAccessLog(new AccessLog()
            .withEnabled(true)
            .withS3BucketName(bucketName)
            .withS3BucketPrefix("elb/" + resource.id));
    }

    private void configureHealthCheck(String elbName) {
        // optimize for high load the instances take longer to response, especially in Multi-AZ,
        // there are multiple ELB instances send health check requests same time
        HealthCheck healthCheck = new HealthCheck()
            .withHealthyThreshold(2)
            .withUnhealthyThreshold(5)
            .withInterval(20)
            .withTimeout(15)
            .withTarget("HTTP:80" + resource.healthCheckURL);
        AWS.elb.elb.configureHealthCheck(new ConfigureHealthCheckRequest(elbName, healthCheck));
    }
}
