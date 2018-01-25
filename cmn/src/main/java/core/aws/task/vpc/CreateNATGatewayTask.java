package core.aws.task.vpc;

import com.amazonaws.services.ec2.model.NatGateway;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.vpc.NATGateway;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("create-nat-gateway")
public class CreateNATGatewayTask extends Task<NATGateway> {
    public CreateNATGatewayTask(NATGateway nat) {
        super(nat);
    }

    @Override
    public void execute(Context context) throws Exception {
        NatGateway natGateway = AWS.getVpc().createNATGateway(resource.subnet.remoteSubnets.first().getSubnetId(), resource.ip);
        resource.remoteNATGateway = natGateway;
    }
}
