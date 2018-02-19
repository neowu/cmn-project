package core.aws.task.elb.v2;

import com.amazonaws.services.elasticloadbalancingv2.model.DeleteListenerRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.v2.ELB;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Action("delete-elb-listener")
public class DeleteELBListenerTask extends Task<ELB> {
    private final Logger logger = LoggerFactory.getLogger(DeleteELBListenerTask.class);
    private final List<String> deletedProtocols;

    public DeleteELBListenerTask(ELB resource, List<String> deletedProtocols) {
        super(resource);
        this.deletedProtocols = deletedProtocols;
    }

    @Override
    public void execute(Context context) throws Exception {
        String elbARN = resource.remoteELB.getLoadBalancerArn();
        deletedProtocols.forEach(protocol -> {
            AWS.getElbV2().listeners(elbARN).stream().filter(listener -> listener.getProtocol().equals(protocol)).map(Listener::getListenerArn).forEach(this::deleteListener);
        });
    }

    private void deleteListener(String listenerARN) {
        DeleteListenerRequest request = new DeleteListenerRequest()
            .withListenerArn(listenerARN);
        logger.info("delete ELB listener, request={}", request);
        AWS.getElbV2().elb.deleteListener(request);
    }
}
