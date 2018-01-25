package core.aws.task.elb;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.ELB;
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
        String elbName = resource.remoteELB.getLoadBalancerName();
        String sgId = resource.securityGroup.remoteSecurityGroup.getGroupId();

        AWS.getElb().updateELBSG(elbName, sgId);
    }
}
