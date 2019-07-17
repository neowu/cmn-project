package core.aws.task.elb.v2;

import com.amazonaws.services.elasticloadbalancingv2.model.ActionTypeEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.AddTagsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Certificate;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateLoadBalancerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.IpAddressType;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancerTypeEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.v2.ELB;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.apache.commons.compress.utils.Sets;

import java.util.Set;

/**
 * @author gabo
 */
@Action("create-elb")
public class CreateELBTask extends Task<ELB> {
    public CreateELBTask(ELB elb) {
        super(elb);
    }

    @Override
    public void execute(Context context) throws Exception {
        ELBTagHelper tags = new ELBTagHelper(context.env);

        CreateLoadBalancerRequest request = new CreateLoadBalancerRequest()
            .withName(resource.name)
            .withScheme(resource.scheme.orElse(null))
            .withType(LoadBalancerTypeEnum.Application)
            .withIpAddressType(IpAddressType.Ipv4)
            .withTags(
                new Tag().withKey("cloud-manager:env").withValue(context.env.name),
                new Tag().withKey("cloud-manager:elb-version").withValue("2")
            );

        if (resource.subnets != null) {
            final Set<String> subnets = Sets.newHashSet();
            resource.subnets.forEach(subnet -> subnet.remoteSubnets.forEach(remoteSubnet -> subnets.add(remoteSubnet.getSubnetId())));
            request.withSecurityGroups(resource.securityGroup.remoteSecurityGroup.getGroupId())
                .withSubnets(subnets);
        }

        resource.remoteELB = AWS.getElbV2().createELB(request);
        createListeners();

        AWS.getElbV2().elb.addTags(new AddTagsRequest()
            .withResourceArns(resource.remoteELB.getLoadBalancerArn())
            .withTags(tags.env(), tags.resourceId(resource.id), tags.name(resource.id)));

        context.output(String.format("elb/%s/DNS", resource.id), resource.remoteELB.getDNSName());
    }

    private void createListeners() throws Exception {
        if (resource.listenHTTP) {
            createListener(80, "HTTP", null);
        }
        if (resource.listenHTTPS) {
            String certARN = resource.amazonCertARN != null ? resource.amazonCertARN : resource.cert.remoteCert.getServerCertificateMetadata().getArn();
            createListener(443, "HTTPS", certARN);
        }
    }

    private void createListener(Integer port, String protocol, String certARN) throws Exception {
        CreateListenerRequest request = new CreateListenerRequest()
            .withPort(port)
            .withProtocol(protocol)
            .withLoadBalancerArn(resource.remoteELB.getLoadBalancerArn())
            .withDefaultActions(new com.amazonaws.services.elasticloadbalancingv2.model.Action().withTargetGroupArn(resource.targetGroup.remoteTG.getTargetGroupArn()).withType(ActionTypeEnum.Forward));

        if (certARN != null) {
            request.withCertificates(new Certificate().withCertificateArn(certARN));
        }
        AWS.getElbV2().createListener(request);
    }
}
