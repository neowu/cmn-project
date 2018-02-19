package core.aws.task.elb.v2;

import com.amazonaws.services.elasticloadbalancingv2.model.AddTagsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.CreateTargetGroupResult;
import com.amazonaws.services.elasticloadbalancingv2.model.TargetTypeEnum;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.v2.TargetGroup;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Action("create-tg")
public class CreateTargetGroupTask extends Task<TargetGroup> {
    private final Logger logger = LoggerFactory.getLogger(CreateTargetGroupTask.class);

    public CreateTargetGroupTask(TargetGroup resource) {
        super(resource);
    }

    @Override
    public void execute(Context context) throws Exception {
        ELBTagHelper tags = new ELBTagHelper(context.env);

        CreateTargetGroupRequest request = new CreateTargetGroupRequest()
            .withVpcId(resource.vpc.remoteVPC.getVpcId())
            .withTargetType(TargetTypeEnum.Instance)
            .withHealthCheckIntervalSeconds(20)
            .withHealthCheckPath(resource.healthCheckURL)
            .withHealthCheckProtocol(resource.protocol())
            .withHealthCheckPort(String.valueOf(resource.port))
            .withHealthCheckTimeoutSeconds(15)
            .withHealthyThresholdCount(2)
            .withUnhealthyThresholdCount(5)
            .withName(resource.name)
            .withPort(resource.port)
            .withProtocol(resource.protocol());

        logger.info("create target group, request={}", request);
        CreateTargetGroupResult result = AWS.getElbV2().elb.createTargetGroup(request);
        if (result.getTargetGroups() == null || result.getTargetGroups().isEmpty()) {
            throw new Error("failed to create target group, name = " + resource.name);
        }
        resource.remoteTG = result.getTargetGroups().get(0);

        AWS.getElbV2().elb.addTags(new AddTagsRequest()
            .withResourceArns(resource.remoteTG.getTargetGroupArn())
            .withTags(tags.env(), tags.resourceId(resource.id), tags.name(resource.id)));
    }
}
