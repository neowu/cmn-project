package core.aws.task.sqs;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.sqs.Queue;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("create-sqs")
public class CreateQueueTask extends Task<Queue> {
    public CreateQueueTask(Queue queue) {
        super(queue);
    }

    @Override
    public void execute(Context context) throws Exception {
        resource.remoteQueueURL = AWS.sqs.createQueue(resource.name);
        context.output("sqs/" + resource.id, "url=" + resource.remoteQueueURL);
    }
}
