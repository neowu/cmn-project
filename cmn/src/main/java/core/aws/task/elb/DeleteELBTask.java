package core.aws.task.elb;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.ELB;
import core.aws.util.Threads;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * @author neo
 */
@Action("del-elb")
public class DeleteELBTask extends Task<ELB> {
    private final Logger logger = LoggerFactory.getLogger(DeleteELBTask.class);

    public DeleteELBTask(ELB elb) {
        super(elb);
    }

    @Override
    public void execute(Context context) throws Exception {
        String elbName = resource.remoteELB.getLoadBalancerName();
        AWS.getElb().deleteELB(elbName);

        logger.info("elb deletion takes time to refresh to other system, wait first");
        if (!resource.remoteELB.getSubnets().isEmpty())
            Threads.sleepRoughly(Duration.ofSeconds(90));  // sleep at least 90s for ELB in VPC
        else
            Threads.sleepRoughly(Duration.ofSeconds(30));

        context.output("elb/" + resource.id, "deletedELB=" + elbName);
    }
}
