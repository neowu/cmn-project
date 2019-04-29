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
        CreateLoadBalancerRequest request = new CreateLoadBalancerRequest()
            .withLoadBalancerName(resource.name)
            .withScheme(resource.scheme.orElse(null))
            .withTags(new Tag().withKey("cloud-manager:env").withValue(context.env.name));

        if (resource.subnet != null) {
            request.withSecurityGroups(resource.securityGroup.remoteSecurityGroup.getGroupId())
                .withSubnets(resource.subnet.remoteSubnets.stream().map(Subnet::getSubnetId).collect(Collectors.toList()));
        } else {
            List<String> zones = AWS.getEc2().availabilityZones();
            request.withAvailabilityZones(zones.get(0));
        }

        if (resource.listenHTTP) {
            request.getListeners().add(new Listener("HTTP", 80, 80));
        }

        if (resource.listenHTTPS) {
            String certARN = resource.amazonCertARN != null ? resource.amazonCertARN : resource.cert.remoteCert.getServerCertificateMetadata().getArn();
            request.getListeners().add(new Listener()
                .withProtocol("HTTPS")
                .withLoadBalancerPort(443)
                .withInstanceProtocol("HTTP")
                .withInstancePort(80)
                .withSSLCertificateId(certARN));
        }

        resource.remoteELB = AWS.getElb().createELB(request);

        configureELB(context.env.region);

        configureHealthCheck();

        context.output(String.format("elb/%s/DNS", resource.id), resource.remoteELB.getDNSName());
    }

    private void configureELB(Regions region) {
        LoadBalancerAttributes attributes = new LoadBalancerAttributes()
            .withConnectionDraining(new ConnectionDraining().withEnabled(Boolean.TRUE).withTimeout(30));

        // enable cross zone load balance for multi-az
        if (resource.subnet != null && resource.subnet.cidrs.size() > 1) {
            attributes.setCrossZoneLoadBalancing(new CrossZoneLoadBalancing().withEnabled(Boolean.TRUE));
        }

        if (resource.accessLogBucket != null) {
            configureAccessLog(attributes, region);
        }

        AWS.getElb().modifyELBAttributes(new ModifyLoadBalancerAttributesRequest()
            .withLoadBalancerName(resource.name)
            .withLoadBalancerAttributes(attributes));
    }

    private void configureAccessLog(LoadBalancerAttributes attributes, Regions region) {
        ELBAccessLogBucketPolicyBuilder builder = new ELBAccessLogBucketPolicyBuilder();
        String bucketName = resource.accessLogBucket.remoteBucket.getName();
        AWS.getS3().s3.setBucketPolicy(new SetBucketPolicyRequest(bucketName, builder.policyText(region, bucketName)));

        attributes.withAccessLog(new AccessLog()
            .withEnabled(Boolean.TRUE)
            .withS3BucketName(bucketName)
            .withS3BucketPrefix("elb/" + resource.id));
    }

    private void configureHealthCheck() {
        // optimize for high load the instances take longer to response, especially in Multi-AZ,
        // there are multiple ELB instances send health check requests same time
        HealthCheck healthCheck = new HealthCheck()
            .withHealthyThreshold(2)
            .withUnhealthyThreshold(5)
            .withInterval(20)
            .withTimeout(15)
            .withTarget("HTTP:80" + resource.healthCheckURL);
        AWS.getElb().elb.configureHealthCheck(new ConfigureHealthCheckRequest(resource.name, healthCheck));
    }
}
