package core.aws.resource.ec2;

import com.amazonaws.services.ec2.model.ImageState;
import core.aws.resource.image.Image;
import core.aws.task.ec2.DeleteImageTask;
import core.aws.util.StreamHelper;
import core.aws.workflow.Tasks;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author neo
 */
class ImageTest {
    private Image image;

    @BeforeEach
    void createAMI() {
        image = new Image("test");
    }

    @Test
    void nextVersionWithoutRemoteImages() {
        assertThat(image.nextVersion()).isEqualTo(1);
    }

    @Test
    void nextVersion() {
        image.remoteImages.put(1, new com.amazonaws.services.ec2.model.Image().withName("image1"));
        image.remoteImages.put(3, new com.amazonaws.services.ec2.model.Image().withName("image3"));
        image.remoteImages.put(2, new com.amazonaws.services.ec2.model.Image().withName("image2"));

        assertThat(image.nextVersion()).isEqualTo(4);
    }

    @Test
    void deleteOldAMI() {
        image.remoteImages.put(1, new com.amazonaws.services.ec2.model.Image().withName("image1").withState(ImageState.Available));
        image.remoteImages.put(2, new com.amazonaws.services.ec2.model.Image().withName("image2").withState(ImageState.Available));
        image.remoteImages.put(5, new com.amazonaws.services.ec2.model.Image().withName("image5").withState(ImageState.Available));
        image.remoteImages.put(3, new com.amazonaws.services.ec2.model.Image().withName("image3").withState(ImageState.Available));
        image.remoteImages.put(4, new com.amazonaws.services.ec2.model.Image().withName("image4").withState(ImageState.Available));

        Tasks tasks = new Tasks();
        image.bakeTasks(tasks, false);

        assertThat(tasks.size()).isEqualTo(2);
        DeleteImageTask task = (DeleteImageTask) tasks.stream().filter(DeleteImageTask.class::isInstance).reduce(StreamHelper.onlyOne()).get();
        assertThat(task.deletedImage.getName()).isEqualTo("image1");
    }
}
