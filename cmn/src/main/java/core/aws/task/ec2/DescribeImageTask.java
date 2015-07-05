package core.aws.task.ec2;

import core.aws.env.Context;
import core.aws.resource.image.Image;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.Map;

/**
 * @author neo
 */
@Action("desc-img")
public class DescribeImageTask extends Task<Image> {
    public DescribeImageTask(Image image) {
        super(image);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "ami/" + resource.id;
        context.output(key, String.format("status=%s", resource.status));
        for (Map.Entry<Integer, String> entry : resource.remoteImageIds.entrySet()) {
            context.output(key, "version=" + entry.getKey() + ", imageId=" + entry.getValue());
        }
    }
}
