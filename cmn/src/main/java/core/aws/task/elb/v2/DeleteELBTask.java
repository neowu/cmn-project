package core.aws.task.elb.v2;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.v2.ELB;
import core.aws.util.Threads;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

@Action("delete-elb")
public class DeleteELBTask extends Task<ELB> {
    private final Logger logger = LoggerFactory.getLogger(DeleteELBTask.class);

    public DeleteELBTask(ELB elb) {
        super(elb);
    }

    @Override
    public void execute(Context context) throws Exception {
        String elbARN = resource.remoteELB.getLoadBalancerArn();
        AWS.getElbV2().deleteELB(elbARN);

        logger.info("elb deletion takes time to refresh to other systems, wait first");
        if (resource.remoteELB.getVpcId() != null) {
            Threads.sleepRoughly(Duration.ofSeconds(90)); // sleep at least 90s for ELB in VPC
        } else {
            Threads.sleepRoughly(Duration.ofSeconds(60));
        }

        context.output("elb-v2/" + resource.id, "deletedELB=" + elbARN);
    }
}
