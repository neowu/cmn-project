package core.aws.task.ec2;

import com.amazonaws.services.ec2.model.DeregisterImageRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.image.Image;
import core.aws.util.Strings;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;

/**
 * @author neo
 */
@Action("del-img")
public class DeleteImageTask extends core.aws.workflow.Task<Image> {
    public final com.amazonaws.services.ec2.model.Image deletedImage;

    public DeleteImageTask(Image image, com.amazonaws.services.ec2.model.Image deletedImage) {
        super(image);
        this.deletedImage = deletedImage;
    }

    @Override
    public void execute(Context context) throws Exception {
        AWS.ec2.ec2.deregisterImage(new DeregisterImageRequest(deletedImage.getImageId()));

        context.output("ami/" + resource.id, Strings.format("deletedImageId={}, deletedImageName={}", deletedImage.getImageId(), deletedImage.getName()));
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource.id)
            .add("deletedImageId", deletedImage.getImageId())
            .add("deletedImageName", deletedImage.getName())
            .toString();
    }
}
