package core.aws.task.ec2;

import com.amazonaws.AmazonServiceException;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.util.Runner;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.time.Duration;

/**
 * @author neo
 */
@Action("del-sg")
public class DeleteSGTask extends Task<SecurityGroup> {
    public DeleteSGTask(SecurityGroup securityGroup) {
        super(securityGroup);
    }

    @Override
    public void execute(Context context) throws Exception {
        new Runner<Void>()
            .maxAttempts(5)
            .retryInterval(Duration.ofSeconds(60))
            .retryOn(e -> e instanceof AmazonServiceException)
            .run(() -> {
                AWS.ec2.deleteSecurityGroup(resource.remoteSecurityGroup.getGroupId());
                return null;
            });

        context.output("sg/" + resource.id, "deletedSGName=" + resource.remoteSecurityGroup.getGroupName());
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource)
            .add("remoteSGName", resource.remoteSecurityGroup.getGroupName())
            .toString();
    }
}
