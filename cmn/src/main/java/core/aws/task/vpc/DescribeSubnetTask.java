package core.aws.task.vpc;

import core.aws.env.Context;
import core.aws.resource.vpc.Subnet;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("desc-subnet")
public class DescribeSubnetTask extends Task<Subnet> {
    public DescribeSubnetTask(Subnet subnet) {
        super(subnet);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "subnet/" + resource.id;
        context.output(key, String.format("status=%s, type=%s, cidrs=%s", resource.status, resource.type, resource.cidrs));

        for (com.amazonaws.services.ec2.model.Subnet remoteSubnet : resource.remoteSubnets) {
            context.output(key, String.format("subnetId=%s, cidr=%s, az=%s",
                remoteSubnet.getSubnetId(),
                remoteSubnet.getCidrBlock(),
                remoteSubnet.getAvailabilityZone()));
        }
    }
}
