package core.aws.resource.vpc;

import core.aws.resource.Resource;
import core.aws.task.vpc.CreateInternetGatewayTask;
import core.aws.task.vpc.DeleteInternetGatewayTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class InternetGateway extends Resource {
    private static final String RESOURCE_ID = "internet-gateway";
    public String remoteInternetGatewayId;
    public VPC vpc;

    public InternetGateway() {
        super(RESOURCE_ID);
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateInternetGatewayTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteInternetGatewayTask(this));
    }
}
