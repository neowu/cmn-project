package core.aws.task;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

/**
 * @author neo
 */
public class TaskBuilderTest {
    TaskBuilder builder = new TaskBuilder(null, null, null);

    @Test
    public void splitResourceId() {
        List<String> ids = builder.resourceIds(null);
        Assert.assertNull(ids);

        ids = builder.resourceIds("id");
        Assert.assertEquals(1, ids.size());
        Assert.assertEquals("id", ids.get(0));

        ids = builder.resourceIds("id1,id2");
        Assert.assertEquals(2, ids.size());
        Assert.assertEquals("id1", ids.get(0));
        Assert.assertEquals("id2", ids.get(1));
    }
}