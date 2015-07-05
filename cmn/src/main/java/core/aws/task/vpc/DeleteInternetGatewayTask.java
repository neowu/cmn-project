package core.aws.task.vpc;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.DeleteInternetGatewayRequest;
import com.amazonaws.services.ec2.model.DetachInternetGatewayRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.vpc.InternetGateway;
import core.aws.util.Runner;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

/**
 * @author neo
 */
@Action("del-internet-gateway")
public class DeleteInternetGatewayTask extends Task<InternetGateway> {
    private final Logger logger = LoggerFactory.getLogger(DeleteInternetGatewayTask.class);

    public DeleteInternetGatewayTask(InternetGateway internetGateway) {
        super(internetGateway);
    }

    @Override
    public void execute(Context context) throws Exception {
        new Runner<Void>()
            .maxAttempts(5)
            .retryInterval(Duration.ofSeconds(60))
            .retryOn(e -> e instanceof AmazonServiceException)
            .run(() -> {
                logger.info("delete internet gateway, internetGatewayId={}", resource.id);

                AWS.vpc.ec2.detachInternetGateway(new DetachInternetGatewayRequest()
                    .withVpcId(resource.vpc.remoteVPC.getVpcId())
                    .withInternetGatewayId(resource.remoteInternetGatewayId));
                AWS.vpc.ec2.deleteInternetGateway(new DeleteInternetGatewayRequest()
                    .withInternetGatewayId(resource.remoteInternetGatewayId));
                return null;
            });
    }
}
