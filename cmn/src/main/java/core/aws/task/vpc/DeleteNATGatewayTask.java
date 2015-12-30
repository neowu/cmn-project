package core.aws.task.vpc;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.vpc.NATGateway;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("del-nat-gateway")
public class DeleteNATGatewayTask extends Task<NATGateway> {
    public DeleteNATGatewayTask(NATGateway nat) {
        super(nat);
    }

    @Override
    public void execute(Context context) throws Exception {
        AWS.vpc.deleteNATGateway(resource.remoteNATGateway.getNatGatewayId());
    }
}
