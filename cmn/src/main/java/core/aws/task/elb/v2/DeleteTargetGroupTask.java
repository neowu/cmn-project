package core.aws.task.elb.v2;

import com.amazonaws.services.elasticloadbalancingv2.model.DeleteTargetGroupRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.v2.TargetGroup;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Action("delete-tg")
public class DeleteTargetGroupTask extends Task<TargetGroup> {
    private final Logger logger = LoggerFactory.getLogger(DeleteTargetGroupTask.class);

    public DeleteTargetGroupTask(TargetGroup resource) {
        super(resource);
    }

    @Override
    public void execute(Context context) throws Exception {
        String tgARN = resource.remoteTG.getTargetGroupArn();
        DeleteTargetGroupRequest request = new DeleteTargetGroupRequest()
            .withTargetGroupArn(tgARN);

        logger.info("delete target group, request={}", request);
        AWS.getElbV2().elb.deleteTargetGroup(request);
    }
}
