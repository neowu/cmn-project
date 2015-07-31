package core.aws.task.as;

import core.aws.resource.as.ASGroup;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author neo
 */
public class DeployASGroupTaskTest {
    DeployASGroupTask task;

    @Before
    public void createDeployASGroupTask() {
        task = new DeployASGroupTask(new ASGroup("test"));
    }

    @Test
    public void capacityDuringDeployment() {
        Assert.assertEquals("add 3 more for current cap", 8, task.capacityDuringDeployment(5, 3));
        Assert.assertEquals("just double the current cap", 4, task.capacityDuringDeployment(2, 1));

        Assert.assertEquals("just double the current cap", 6, task.capacityDuringDeployment(3, 3));

        Assert.assertEquals("lift cap to target", 4, task.capacityDuringDeployment(0, 4));
        Assert.assertEquals("lift cap to target", 3, task.capacityDuringDeployment(0, 3));
        Assert.assertEquals("lift cap to target", 2, task.capacityDuringDeployment(0, 2));

        Assert.assertEquals("lift with target", 3, task.capacityDuringDeployment(1, 2));
        Assert.assertEquals("lift with target", 8, task.capacityDuringDeployment(3, 5));
        Assert.assertEquals("lift with target", 8, task.capacityDuringDeployment(4, 5));
        Assert.assertEquals("lift with target", 9, task.capacityDuringDeployment(4, 6));
    }
}