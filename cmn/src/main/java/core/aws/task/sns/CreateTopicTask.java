package core.aws.task.sns;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.sns.Topic;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("create-sns")
public class CreateTopicTask extends Task<Topic> {
    public CreateTopicTask(Topic topic) {
        super(topic);
    }

    @Override
    public void execute(Context context) throws Exception {
        resource.remoteTopicARN = AWS.sns.createTopic(resource.name);
        context.output("sns/" + resource.id, "arn=" + resource.remoteTopicARN);
    }
}
