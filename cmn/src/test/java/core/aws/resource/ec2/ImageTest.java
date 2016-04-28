package core.aws.resource.ec2;

import com.amazonaws.services.ec2.model.ImageState;
import core.aws.resource.image.Image;
import core.aws.task.ec2.DeleteImageTask;
import core.aws.util.StreamHelper;
import core.aws.workflow.Tasks;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author neo
 */
public class ImageTest {
    Image image;

    @Before
    public void createAMI() {
        image = new Image("test");
    }

    @Test
    public void nextVersionWithoutRemoteImages() {
        assertThat(image.nextVersion(), equalTo(1));
    }

    @Test
    public void nextVersion() {
        image.remoteImages.put(1, new com.amazonaws.services.ec2.model.Image().withName("image1"));
        image.remoteImages.put(3, new com.amazonaws.services.ec2.model.Image().withName("image3"));
        image.remoteImages.put(2, new com.amazonaws.services.ec2.model.Image().withName("image2"));

        assertThat(image.nextVersion(), equalTo(4));
    }

    @Test
    public void deleteOldAMI() {
        image.remoteImages.put(1, new com.amazonaws.services.ec2.model.Image().withName("image1").withState(ImageState.Available));
        image.remoteImages.put(2, new com.amazonaws.services.ec2.model.Image().withName("image2").withState(ImageState.Available));
        image.remoteImages.put(5, new com.amazonaws.services.ec2.model.Image().withName("image5").withState(ImageState.Available));
        image.remoteImages.put(3, new com.amazonaws.services.ec2.model.Image().withName("image3").withState(ImageState.Available));
        image.remoteImages.put(4, new com.amazonaws.services.ec2.model.Image().withName("image4").withState(ImageState.Available));

        Tasks tasks = new Tasks();
        image.bakeTasks(tasks, false);

        assertThat(tasks.size(), equalTo(2));
        DeleteImageTask task = (DeleteImageTask) tasks.stream().filter(t -> t instanceof DeleteImageTask).reduce(StreamHelper.onlyOne()).get();
        assertThat(task.deletedImage.getName(), equalTo("image1"));
    }
}
