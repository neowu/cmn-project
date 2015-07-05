package core.aws.task.ec2;

import core.aws.env.Context;
import core.aws.resource.ec2.Instance;
import core.aws.resource.ec2.KeyPair;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("desc-instance")
public class DescribeInstanceTask extends Task<Instance> {
    public DescribeInstanceTask(Instance instance) {
        super(instance);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "instance/" + resource.id;
        context.output(key, String.format("status=%s, type=%s, keyFile=%s",
            resource.status,
            resource.instanceType,
            KeyPair.keyFile(resource.keyPair.name, context.env)));

        int index = 0;
        for (com.amazonaws.services.ec2.model.Instance remoteInstance : resource.remoteInstances) {
            context.output(key, String.format("instanceId=%s, i=%s, state=%s, publicDNS=%s, publicIP=%s, privateIP=%s",
                remoteInstance.getInstanceId(),
                index++,
                remoteInstance.getState().getName(),
                remoteInstance.getPublicDnsName(),
                remoteInstance.getPublicIpAddress(),
                remoteInstance.getPrivateIpAddress()));
        }
    }
}
