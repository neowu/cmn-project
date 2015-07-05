package core.aws.task.ec2;

import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.image.Image;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;

/**
 * @author neo
 */
@Action("del-img")
public class DeleteImageTask extends core.aws.workflow.Task<Image> {
    public final String imageId;

    public DeleteImageTask(Image image, String imageId) {
        super(image);
        this.imageId = imageId;
    }

    @Override
    public void execute(Context context) throws Exception {
        AWS.ec2.ec2.deregisterImage(new DeregisterImageRequest(imageId));

        context.output("ami/" + resource.id, String.format("deletedImageId=%s", imageId));
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add("image", resource)
            .add("imageId", imageId)
            .toString();
    }
}
