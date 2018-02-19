package core.aws.task.elb.v2;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.v2.ELB;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("update-elb")
public class UpdateELBSGTask extends Task<ELB> {
    public UpdateELBSGTask(ELB elb) {
        super(elb);
    }

    @Override
    public void execute(Context context) throws Exception {
        String elbARN = resource.remoteELB.getLoadBalancerArn();
        String sgId = resource.securityGroup.remoteSecurityGroup.getGroupId();

        AWS.getElbV2().updateELBSG(elbARN, sgId);
    }
}
