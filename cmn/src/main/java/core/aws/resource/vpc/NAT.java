package core.aws.resource.vpc;

import com.amazonaws.services.ec2.model.Instance;
import core.aws.resource.Resource;
import core.aws.resource.ec2.KeyPair;
import core.aws.resource.image.AMI;
import core.aws.task.vpc.CreateNATTask;
import core.aws.task.vpc.DeleteNATTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class NAT extends Resource {
    public static final String RESOURCE_ID = "nat";
    public Instance remoteInstance;
    public Subnet publicSubnet;
    public AMI image;
    public VPC vpc;
    public KeyPair keyPair;

    public NAT(String id) {
        super(id);
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateNATTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteNATTask(this));
    }
}
