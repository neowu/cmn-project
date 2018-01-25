package core.aws.task.vpc;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.vpc.Subnet;
import core.aws.util.Runner;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * @author neo
 */
@Action("del-subnet")
public class DeleteSubnetTask extends Task<Subnet> {
    private final Logger logger = LoggerFactory.getLogger(DeleteSubnetTask.class);

    public DeleteSubnetTask(Subnet subnet) {
        super(subnet);
    }

    @Override
    public void execute(Context context) throws Exception {
        String key = "subnet/" + resource.id;

        for (com.amazonaws.services.ec2.model.Subnet remoteSubnet : resource.remoteSubnets) {
            new Runner<Void>()
                .maxAttempts(5)
                .retryInterval(Duration.ofSeconds(60))
                .retryOn(e -> e instanceof AmazonServiceException)
                .run(() -> {
                    logger.info("delete subnet, subnetId={}, cidr={}", remoteSubnet.getSubnetId(), remoteSubnet.getCidrBlock());
                    AWS.getVpc().ec2.deleteSubnet(new DeleteSubnetRequest().withSubnetId(remoteSubnet.getSubnetId()));
                    context.output(key, String.format("deletedSubnetId=%s, cidr=%s", remoteSubnet.getSubnetId(), remoteSubnet.getCidrBlock()));
                    return null;
                });
        }
    }
}
