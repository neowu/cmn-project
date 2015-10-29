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

import java.util.List;

/**
 * @author neo
 */
@Action("create-elb-listener")
public class CreateELBListenerTask extends Task<ELB> {
    private final Logger logger = LoggerFactory.getLogger(CreateELBListenerTask.class);
    private final List<String> addedProtocols;

    public CreateELBListenerTask(ELB elb, List<String> addedProtocols) {
        super(elb);
        this.addedProtocols = addedProtocols;
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
            request.getListeners().add(new Listener()
                .withProtocol("HTTPS")
                .withLoadBalancerPort(443)
                .withInstanceProtocol("HTTP")
                .withInstancePort(80)
                .withSSLCertificateId(resource.cert.remoteCert.getServerCertificateMetadata().getArn()));
        }

        logger.info("create ELB listeners, request={}", request);
        AWS.elb.elb.createLoadBalancerListeners(request);
    }
}
