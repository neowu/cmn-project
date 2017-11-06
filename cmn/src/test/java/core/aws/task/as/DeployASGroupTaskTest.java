package core.aws.task.as;

import core.aws.resource.as.ASGroup;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author neo
 */
class DeployASGroupTaskTest {
    private DeployASGroupTask task;

    @BeforeEach
    void createDeployASGroupTask() {
        task = new DeployASGroupTask(new ASGroup("test"));
    }

    @Test
    void capacityDuringDeployment() {
        assertEquals(8, task.capacityDuringDeployment(5, 3), "add 3 more for current cap");
        assertEquals(4, task.capacityDuringDeployment(2, 1), "just double the current cap");

        assertEquals(6, task.capacityDuringDeployment(3, 3), "just double the current cap");

        assertEquals(4, task.capacityDuringDeployment(0, 4), "lift cap to target");
        assertEquals(3, task.capacityDuringDeployment(0, 3), "lift cap to target");
        assertEquals(2, task.capacityDuringDeployment(0, 2), "lift cap to target");

        assertEquals(3, task.capacityDuringDeployment(1, 2), "lift with target");
        assertEquals(8, task.capacityDuringDeployment(3, 5), "lift with target");
        assertEquals(8, task.capacityDuringDeployment(4, 5), "lift with target");
        assertEquals(9, task.capacityDuringDeployment(4, 6), "lift with target");
    }
}
