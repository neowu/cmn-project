package core.aws.task.s3;

import com.amazonaws.services.s3.model.TagSet;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.s3.Bucket;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("create-s3")
public class CreateBucketTask extends Task<Bucket> {
    public CreateBucketTask(Bucket bucket) {
        super(bucket);
    }

    @Override
    public void execute(Context context) throws Exception {
        resource.remoteBucket = AWS.getS3().createBucket(resource.name);
        context.output("s3/" + resource.id, "bucketName=" + resource.remoteBucket.getName());

        TagSet tagSet = new TagSet();
        tagSet.setTag("cloud-manager:env", context.env.name);
        tagSet.setTag("cloud-manager:env:" + context.env.name + ":resource-id", resource.id);
        AWS.getS3().createTags(resource.name, tagSet);
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource)
            .add("bucket", resource.name)
            .toString();
    }
}
