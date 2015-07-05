package core.aws.task.sqs;

import core.aws.env.Context;
import core.aws.resource.sqs.Queue;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("desc-sqs")
public class DescribeQueueTask extends Task<Queue> {
    public DescribeQueueTask(Queue queue) {
        super(queue);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "sqs/" + resource.id;
        context.output(key, String.format("status=%s, name=%s",
            resource.status, resource.name));

        if (resource.remoteQueueURL != null) {
            context.output(key, "url=" + resource.remoteQueueURL);
        }
    }
}
