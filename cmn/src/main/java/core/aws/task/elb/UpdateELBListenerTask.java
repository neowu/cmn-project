package core.aws.task.elb;

import com.amazonaws.services.elasticloadbalancing.model.CreateLoadBalancerListenersRequest;
import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerListenersRequest;
import com.amazonaws.services.elasticloadbalancing.model.Listener;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.ELB;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author neo
 */
@Action("update-elb")
public class UpdateELBListenerTask extends Task<ELB> {
    private final Logger logger = LoggerFactory.getLogger(UpdateELBListenerTask.class);

    public UpdateELBListenerTask(ELB elb) {
        super(elb);
    }

    @Override
    public void execute(Context context) throws Exception {
        String elbName = resource.remoteELB.getLoadBalancerName();

        CreateLoadBalancerListenersRequest createRequest = new CreateLoadBalancerListenersRequest()
            .withLoadBalancerName(elbName);
        DeleteLoadBalancerListenersRequest deleteRequest = new DeleteLoadBalancerListenersRequest()
            .withLoadBalancerName(elbName);

        if (resource.httpListenerAdded()) {
            createRequest.getListeners().add(new Listener("HTTP", 80, 80));
        }

        if (resource.httpListenerRemoved()) {
            deleteRequest.getLoadBalancerPorts().add(80);
        }

        if (resource.httpsListenerRemoved() || resource.httpsCertChanged()) {
            deleteRequest.getLoadBalancerPorts().add(443);
        }

        if (resource.httpsListenerAdded() || resource.httpsCertChanged()) {
            createRequest.getListeners().add(new Listener()
                .withProtocol("HTTPS")
                .withLoadBalancerPort(443)
                .withInstanceProtocol("HTTP")
                .withInstancePort(80)
                .withSSLCertificateId(resource.cert.remoteCert.getServerCertificateMetadata().getArn()));
        }

        if (!deleteRequest.getLoadBalancerPorts().isEmpty()) {
            logger.info("delete ELB listeners, request={}", deleteRequest);
            AWS.elb.elb.deleteLoadBalancerListeners(deleteRequest);
        }

        if (!createRequest.getListeners().isEmpty()) {
            logger.info("create ELB listeners, request={}", createRequest);
            AWS.elb.elb.createLoadBalancerListeners(createRequest);
        }
    }
}
