package core.aws.plan.ec2;

import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.UserIdGroupPair;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.task.ec2.DeleteSGRuleTask;
import core.aws.task.ec2.DeleteSGTask;
import core.aws.workflow.Tasks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author neo
 */
class SGTaskPlannerTest {
    @Test
    void linkDeleteSGRuleTask() {
        Tasks tasks = new Tasks();

        SecurityGroup adminSG = new SecurityGroup("admin");
        adminSG.remoteSecurityGroup = new com.amazonaws.services.ec2.model.SecurityGroup().withGroupId("admin");
        SecurityGroup webSG = new SecurityGroup("web");
        webSG.remoteSecurityGroup = new com.amazonaws.services.ec2.model.SecurityGroup().withGroupId("web")
                                                                                        .withIpPermissions(new IpPermission().withUserIdGroupPairs(new UserIdGroupPair().withGroupId("admin")));

        DeleteSGTask deleteAdminTask = tasks.add(new DeleteSGTask(adminSG));

        DeleteSGTask deleteWebTask = tasks.add(new DeleteSGTask(webSG));
        DeleteSGRuleTask deleteWebRuleTask = tasks.add(new DeleteSGRuleTask(webSG, webSG.remoteSecurityGroup.getIpPermissions()));
        deleteWebTask.dependsOn(deleteWebTask);

        new SGTaskPlanner(tasks).plan();

        assertTrue(deleteAdminTask.dependencies.contains(deleteWebRuleTask));
    }
}
