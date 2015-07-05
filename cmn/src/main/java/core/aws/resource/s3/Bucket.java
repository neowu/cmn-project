package core.aws.resource.s3;

import core.aws.resource.Resource;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.task.s3.CreateBucketTask;
import core.aws.task.s3.DeleteBucketTask;
import core.aws.task.s3.DescribeBucketTask;
import core.aws.util.Asserts;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class Bucket extends Resource {
    public String name;
    public com.amazonaws.services.s3.model.Bucket remoteBucket;

    public Bucket(String id) {
        super(id);
    }

    @Override
    public void validate(Resources resources) {
        if (status == ResourceStatus.LOCAL_ONLY) {
            Asserts.isTrue(name.length() <= 63, "max length of bucket name is 63");
            Asserts.isFalse(name.contains("_"), "bucket name can not contain '_'");
        }
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateBucketTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteBucketTask(this));
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeBucketTask(this));
    }
}
