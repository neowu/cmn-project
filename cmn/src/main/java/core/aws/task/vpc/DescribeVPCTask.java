package core.aws.task.vpc;

import core.aws.env.Context;
import core.aws.resource.vpc.VPC;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("desc-vpc")
public class DescribeVPCTask extends Task<VPC> {
    public DescribeVPCTask(VPC vpc) {
        super(vpc);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "vpc/" + resource.id;
        context.output(key, String.format("status=%s", resource.status));
        if (resource.remoteVPC != null)
            context.output(key, String.format("vpcId=%s, cidr=%s",
                resource.remoteVPC.getVpcId(),
                resource.remoteVPC.getCidrBlock()));
    }
}
