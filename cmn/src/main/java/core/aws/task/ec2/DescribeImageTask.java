package core.aws.task.ec2;

import core.aws.env.Context;
import core.aws.resource.image.Image;
import core.aws.util.Strings;
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
        context.output(key, Strings.format("status={}", resource.status));
        for (Map.Entry<Integer, com.amazonaws.services.ec2.model.Image> entry : resource.remoteImages.entrySet()) {
            context.output(key, Strings.format("version={}, name={}, imageId={}", entry.getKey(), entry.getValue().getName(), entry.getValue().getImageId()));
        }
    }
}
