package core.aws.resource.vpc;

import com.amazonaws.services.ec2.model.Vpc;
import core.aws.resource.Resource;
import core.aws.task.vpc.CreateVPCTask;
import core.aws.task.vpc.DeleteVPCTask;
import core.aws.task.vpc.DescribeVPCTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class VPC extends Resource {
    private static final String RESOURCE_ID = "vpc";
    public Vpc remoteVPC;

    public VPC() {
        super(RESOURCE_ID);
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateVPCTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteVPCTask(this));
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeVPCTask(this));
    }
}
