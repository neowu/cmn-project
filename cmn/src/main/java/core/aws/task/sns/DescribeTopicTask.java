package core.aws.task.sns;

import core.aws.env.Context;
import core.aws.resource.sns.Topic;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("desc-sns")
public class DescribeTopicTask extends Task<Topic> {
    public DescribeTopicTask(Topic topic) {
        super(topic);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "sns/" + resource.id;
        context.output(key, String.format("status=%s, name=%s",
            resource.status, resource.name));

        if (resource.remoteTopicARN != null) {
            context.output(key, "arn=" + resource.remoteTopicARN);
        }
    }
}
