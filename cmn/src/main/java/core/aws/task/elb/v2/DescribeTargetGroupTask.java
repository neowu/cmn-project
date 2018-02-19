package core.aws.task.elb.v2;

import core.aws.env.Context;
import core.aws.resource.elb.v2.TargetGroup;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

@Action("describe-tg")
public class DescribeTargetGroupTask extends Task<TargetGroup> {
    public DescribeTargetGroupTask(TargetGroup resource) {
        super(resource);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "tg/" + resource.id;
        context.output(key, String.format("name=%s, health-check=%s", resource.name, resource.healthCheckURL));

        if (resource.remoteTG != null) {
            context.output(key, String.format("arn=%s", resource.remoteTG.getTargetGroupArn()));
        }
    }
}
