package core.aws.task.elb;

import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.ListenerDescription;
import core.aws.env.Context;
import core.aws.resource.elb.ELB;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("desc-elb")
public class DescribeELBTask extends Task<ELB> {
    public DescribeELBTask(ELB elb) {
        super(elb);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "elb/" + resource.id;
        context.output(key, String.format("status=%s, http=%s, https=%s",
            resource.status, resource.listenHTTP, resource.listenHTTPS));

        if (resource.remoteELB != null) {
            context.output(key, "dns=" + resource.remoteELB.getDNSName());
            for (ListenerDescription description : resource.remoteELB.getListenerDescriptions()) {
                Listener listener = description.getListener();
                context.output(key, String.format("listener=%s:%d=>%s:%d, cert=%s",
                    listener.getProtocol(),
                    listener.getLoadBalancerPort(),
                    listener.getInstanceProtocol(),
                    listener.getInstancePort(),
                    listener.getSSLCertificateId()));
            }
        }
    }
}
