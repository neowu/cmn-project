package core.aws.resource.vpc;

import com.amazonaws.services.ec2.model.NatGateway;
import core.aws.resource.Resource;
import core.aws.task.vpc.CreateNATGatewayTask;
import core.aws.task.vpc.DeleteNATGatewayTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class NATGateway extends Resource {
    private static final String RESOURCE_ID = "nat-gateway";
    public NatGateway remoteNATGateway;
    public Subnet subnet;
    public String ip;

    public NATGateway() {
        super(RESOURCE_ID);
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateNATGatewayTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteNATGatewayTask(this));
    }
}
