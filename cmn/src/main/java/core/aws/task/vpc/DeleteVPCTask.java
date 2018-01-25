package core.aws.task.vpc;

import com.amazonaws.services.ec2.model.DeleteVpcRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.vpc.VPC;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author neo
 */
@Action("del-vpc")
public class DeleteVPCTask extends Task<VPC> {
    private final Logger logger = LoggerFactory.getLogger(DeleteVPCTask.class);

    public DeleteVPCTask(VPC vpc) {
        super(vpc);
    }

    @Override
    public void execute(Context context) throws Exception {
        String vpcId = resource.remoteVPC.getVpcId();
        logger.info("delete vpc, vpcId={}", vpcId);
        AWS.getVpc().ec2.deleteVpc(new DeleteVpcRequest(vpcId));
        context.output("vpc/" + resource.id, "deletedVPCId=" + vpcId);
    }
}
