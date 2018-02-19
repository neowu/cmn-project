package core.aws.task.elb.v2;

import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.v2.ELB;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;

@Action("describe-elb")
public class DescribeELBTask extends Task<ELB> {
    public DescribeELBTask(ELB resource) {
        super(resource);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "elb-v2/" + resource.id;
        context.output(key, String.format("status=%s, http=%s, https=%s",
            resource.status, resource.listenHTTP, resource.listenHTTPS));

        if (resource.remoteELB != null) {
            context.output(key, "dns=" + resource.remoteELB.getDNSName());
            List<Listener> listeners = AWS.getElbV2().listeners(resource.remoteELB.getLoadBalancerArn());
            listeners.forEach(listener -> {
                context.output(key, String.format("listener=%s:%d", listener.getProtocol(), listener.getPort()));
                listener.getDefaultActions().forEach(action -> context.output(key, String.format("targetGroup=%s, type=%s", action.getTargetGroupArn(), action.getType())));
            });
        }
    }
}
