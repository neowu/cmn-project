package core.aws.task.ec2;

import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.UserIdGroupPair;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;

/**
 * @author neo
 */
@Action("del-sg-rule")
public class DeleteSGRuleTask extends Task<SecurityGroup> {
    public final List<IpPermission> deletedIngressRules;

    public DeleteSGRuleTask(SecurityGroup securityGroup, List<IpPermission> deletedIngressRules) {
        super(securityGroup);
        this.deletedIngressRules = deletedIngressRules;
    }

    @Override
    public void execute(Context context) throws Exception {
        for (IpPermission permission : deletedIngressRules) {
            for (UserIdGroupPair userGroup : permission.getUserIdGroupPairs()) {
                // it's not allowed to put both groupName and groupId in request
                userGroup.setGroupName(null);
            }
        }

        AWS.ec2.deleteSGIngressRules(resource.remoteSecurityGroup.getGroupId(), deletedIngressRules);
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource)
            .add("remoteSG", resource.remoteSecurityGroup.getGroupName())
            .add("remoteRules", deletedIngressRules)
            .toString();
    }
}
