package core.aws.task.linux;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;

/**
 * @author neo
 */
public class AnsibleProvisionerTest {
    @Test
    public void ansibleCommand() {
        AnsibleProvisioner provisioner = new AnsibleProvisioner(null, null, null, null);
        provisioner.additionalVariables.put("tomcat_service_state", "stopped");
        String command = provisioner.ansibleCommand();

        assertThat(command, containsString("-e '{\"tomcat_service_state\":\"stopped\"}'"));
    }
}