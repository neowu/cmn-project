package core.aws.task.sns;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.sns.Topic;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("del-sns")
public class DeleteTopicTask extends Task<Topic> {
    public DeleteTopicTask(Topic topic) {
        super(topic);
    }

    @Override
    public void execute(Context context) throws Exception {
        AWS.sns.deleteTopic(resource.remoteTopicARN);

        context.output("sns/" + resource.id, "deletedTopic=" + resource.remoteTopicARN);
    }
}
