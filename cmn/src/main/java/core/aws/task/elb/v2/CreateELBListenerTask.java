package core.aws.task.elb.v2;

import com.amazonaws.services.elasticloadbalancingv2.model.ActionTypeEnum;
import com.amazonaws.services.elasticloadbalancingv2.model.Certificate;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateListenerRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.v2.ELB;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Action("create-elb-listener")
public class CreateELBListenerTask extends Task<ELB> {
    private final Logger logger = LoggerFactory.getLogger(CreateELBListenerTask.class);
    private final Set<String> addedProtocols;

    public CreateELBListenerTask(ELB resource, List<String> addedProtocols) {
        super(resource);
        this.addedProtocols = new HashSet<>(addedProtocols);
    }

    @Override
    public void execute(Context context) throws Exception {
        if (addedProtocols.contains("HTTP")) {
            logger.info("create elb http listener");
            createListener(80, "HTTP", null);
        }
        if (addedProtocols.contains("HTTPS")) {
            logger.info("create elb https listener");
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
