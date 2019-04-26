package core.aws.task.elb;

import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerListenersRequest;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.ELB;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author neo
 */
@Action("create-elb-listener")
public class CreateELBListenerTask extends Task<ELB> {
    private final Logger logger = LoggerFactory.getLogger(CreateELBListenerTask.class);
    private final Set<String> addedProtocols;

    public CreateELBListenerTask(ELB elb, List<String> addedProtocols) {
        super(elb);
        this.addedProtocols = new HashSet<>(addedProtocols);
    }

    @Override
    public void execute(Context context) throws Exception {
        String elbName = resource.remoteELB.getLoadBalancerName();

        CreateLoadBalancerListenersRequest request = new CreateLoadBalancerListenersRequest()
            .withLoadBalancerName(elbName);

        if (addedProtocols.contains("HTTP")) {
            request.getListeners().add(new Listener("HTTP", 80, 80));
        }

        if (addedProtocols.contains("HTTPS")) {
            String certARN = resource.amazonCertARN != null ? resource.amazonCertARN : resource.cert.remoteCert.getServerCertificateMetadata().getArn();
            request.getListeners().add(new Listener()
                .withProtocol("HTTPS")
                .withLoadBalancerPort(443)
                .withInstanceProtocol("HTTP")
                .withInstancePort(80)
                .withSSLCertificateId(certARN));
        }

        logger.info("create ELB listeners, request={}", request);
        AWS.getElb().elb.createLoadBalancerListeners(request);
    }
}
