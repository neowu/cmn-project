package core.aws.task.vpc;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.vpc.NAT;
import core.aws.task.ec2.KeyPairHelper;
import core.aws.util.Lists;
import core.aws.util.Threads;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.time.Duration;

/**
 * @author neo
 */
@Action("del-nat")
public class DeleteNATTask extends Task<NAT> {
    public DeleteNATTask(NAT nat) {
        super(nat);
    }

    @Override
    public void execute(Context context) throws Exception {
        KeyPairHelper keyPairHelper = new KeyPairHelper(context.env);

        keyPairHelper.deleteKeyPair(resource.remoteInstance.getKeyName());

        AWS.ec2.terminateInstances(Lists.newArrayList(resource.remoteInstance.getInstanceId()));

        Threads.sleepRoughly(Duration.ofSeconds(5)); // sg needs time to refresh dependencies,

        String sgId = resource.remoteInstance.getSecurityGroups().get(0).getGroupId();
        AWS.ec2.deleteSecurityGroup(sgId);
    }
}
