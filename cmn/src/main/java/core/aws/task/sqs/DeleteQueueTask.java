package core.aws.task.sqs;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.sqs.Queue;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("del-sqs")
public class DeleteQueueTask extends Task<Queue> {
    public DeleteQueueTask(Queue queue) {
        super(queue);
    }

    @Override
    public void execute(Context context) throws Exception {
        AWS.sqs.deleteQueue(resource.remoteQueueURL);

        context.output("sqs/" + resource.id, "deletedQueue=" + resource.remoteQueueURL);
    }
}
