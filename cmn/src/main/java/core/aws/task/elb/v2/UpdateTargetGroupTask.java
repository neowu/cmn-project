package core.aws.task.elb.v2;

import com.amazonaws.services.elasticloadbalancingv2.model.ModifyTargetGroupRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.v2.TargetGroup;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Action("update-tg")
public class UpdateTargetGroupTask extends Task<TargetGroup> {
    private final Logger logger = LoggerFactory.getLogger(UpdateTargetGroupTask.class);

    public UpdateTargetGroupTask(TargetGroup resource) {
        super(resource);
    }

    @Override
    public void execute(Context context) throws Exception {
        if (changed()) {
            ModifyTargetGroupRequest request = new ModifyTargetGroupRequest()
                .withTargetGroupArn(resource.remoteTG.getTargetGroupArn())
                .withHealthCheckPath(resource.healthCheckURL)
                .withHealthCheckProtocol(resource.protocol)
                .withHealthCheckPort(String.valueOf(resource.port));
            logger.info("update target group, name={}, request={}", resource.name, request);
            AWS.getElbV2().elb.modifyTargetGroup(request);
        }
    }

    private boolean changed() {
        return !resource.remoteTG.getHealthCheckPath().equals(resource.healthCheckURL)
            || !resource.remoteTG.getPort().equals(resource.port)
            || !resource.remoteTG.getProtocol().equalsIgnoreCase(resource.protocol);
    }
}
