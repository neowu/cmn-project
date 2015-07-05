package core.aws.task.ec2;

import core.aws.env.Context;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("desc-sg")
public class DescribeSGTask extends Task<SecurityGroup> {
    public DescribeSGTask(SecurityGroup securityGroup) {
        super(securityGroup);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "sg/" + resource.id;
        context.output(key, String.format("status=%s", resource.status));

        com.amazonaws.services.ec2.model.SecurityGroup remoteSecurityGroup = resource.remoteSecurityGroup;
        if (remoteSecurityGroup != null) {
            context.output(key, String.format("sgId=%s, name=%s",
                remoteSecurityGroup.getGroupId(),
                remoteSecurityGroup.getGroupName()));
        }
    }
}
