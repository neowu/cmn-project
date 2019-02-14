package core.aws.task.ec2;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.ec2.InstanceProfile;
import core.aws.util.Threads;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.time.Duration;

/**
 * @author neo
 */
@Action("create-instance-profile")
public class CreateInstanceProfileTask extends Task<InstanceProfile> {
    public CreateInstanceProfileTask(InstanceProfile instanceProfile) {
        super(instanceProfile);
    }

    @Override
    public void execute(Context context) throws Exception {
        resource.remoteInstanceProfile = AWS.getIam().createInstanceProfile(resource.path, resource.name);

        // wait a bit, instance profile usually takes time to be available
        Threads.sleepRoughly(Duration.ofSeconds(10));
    }
}
