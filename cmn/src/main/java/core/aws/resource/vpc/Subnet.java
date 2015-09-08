package core.aws.resource.vpc;

import core.aws.resource.Resource;
import core.aws.task.vpc.CreateSubnetTask;
import core.aws.task.vpc.DeleteSubnetTask;
import core.aws.task.vpc.DescribeSubnetTask;
import core.aws.util.Strings;
import core.aws.workflow.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author neo
 */
public class Subnet extends Resource {
    public final SortedSet<com.amazonaws.services.ec2.model.Subnet> remoteSubnets = new TreeSet<>((subnet1, subnet2) -> Strings.compare(subnet1.getAvailabilityZone(), subnet2.getAvailabilityZone()));
    public final List<String> cidrs = new ArrayList<>();
    public SubnetType type;
    public VPC vpc;
    public RouteTable routeTable;

    public Subnet(String id) {
        super(id);
    }

    public com.amazonaws.services.ec2.model.Subnet firstRemoteSubnet() {
        return remoteSubnets.first();
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateSubnetTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteSubnetTask(this));
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeSubnetTask(this));
    }
}
