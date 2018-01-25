package core.aws.task.s3;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.s3.Bucket;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("del-s3")
public class DeleteBucketTask extends Task<Bucket> {
    public DeleteBucketTask(Bucket bucket) {
        super(bucket);
    }

    @Override
    public void execute(Context context) throws Exception {
        String bucketName = resource.remoteBucket.getName();

        AWS.getS3().deleteAll(bucketName);
        AWS.getS3().deleteBucket(bucketName);

        context.output("s3/" + resource.id, "deletedBucket=" + bucketName);
    }
}
