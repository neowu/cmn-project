package core.aws.task.elb;

import com.amazonaws.services.elasticloadbalancing.model.DeleteLoadBalancerListenersRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.ELB;
import core.aws.util.Threads;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;

/**
 * @author neo
 */
@Action("delete-elb-listener")
public class DeleteELBListenerTask extends Task<ELB> {
    private final Logger logger = LoggerFactory.getLogger(DeleteELBListenerTask.class);
    private final List<String> deletedProtocols;

    public DeleteELBListenerTask(ELB elb, List<String> deletedProtocols) {
        super(elb);
        this.deletedProtocols = deletedProtocols;
    }

    @Override
    public void execute(Context context) throws Exception {
        String elbName = resource.remoteELB.getLoadBalancerName();

        DeleteLoadBalancerListenersRequest request = new DeleteLoadBalancerListenersRequest()
            .withLoadBalancerName(elbName);

        if (deletedProtocols.contains("HTTP")) {
            request.withLoadBalancerPorts(80);
        }
        if (deletedProtocols.contains("HTTPS")) {
            request.withLoadBalancerPorts(443);
        }

        logger.info("delete ELB listeners, request={}", request);
        AWS.elb.elb.deleteLoadBalancerListeners(request);

        if (deletedProtocols.contains("HTTPS")) {
            Threads.sleepRoughly(Duration.ofSeconds(20)); // wait 20s to make change visible to IAM cert system
        }
    }
}
