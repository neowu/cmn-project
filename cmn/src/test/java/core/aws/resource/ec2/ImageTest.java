package core.aws.resource.ec2;

import core.aws.resource.image.Image;
import core.aws.task.ec2.DeleteImageTask;
import core.aws.util.StreamHelper;
import core.aws.workflow.Tasks;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

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
        image.remoteImageIds.put(1, "image1");
        image.remoteImageIds.put(3, "image3");
        image.remoteImageIds.put(2, "image2");

        assertThat(image.nextVersion(), equalTo(4));
    }

    @Test
    public void deleteOldAMI() {
        image.remoteImageIds.put(1, "image1");
        image.remoteImageIds.put(2, "image2");
        image.remoteImageIds.put(3, "image3");
        image.remoteImageIds.put(4, "image4");
        image.remoteImageIds.put(5, "image5");

        Tasks tasks = new Tasks();
        while (image.remoteImageIds.size() >= 5) {
            Map.Entry<Integer, String> entry = image.remoteImageIds.pollFirstEntry();
            tasks.add(new DeleteImageTask(image, entry.getValue()));
        }

        assertThat(tasks.size(), equalTo(1));
        DeleteImageTask task = (DeleteImageTask) tasks.stream().reduce(StreamHelper.onlyOne()).get();
        assertThat(task.imageId, equalTo("image1"));
    }
}
