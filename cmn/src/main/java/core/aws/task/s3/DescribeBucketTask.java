package core.aws.task.s3;

import core.aws.env.Context;
import core.aws.resource.s3.Bucket;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("desc-s3")
public class DescribeBucketTask extends Task<Bucket> {
    public DescribeBucketTask(Bucket bucket) {
        super(bucket);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "s3/" + resource.id;
        context.output(key, String.format("status=%s, bucketName=%s", resource.status, resource.name));
    }
}
