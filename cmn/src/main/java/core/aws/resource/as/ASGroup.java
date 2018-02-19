package core.aws.resource.as;

import core.aws.resource.Resource;
import core.aws.resource.Resources;
import core.aws.resource.ServerResource;
import core.aws.resource.elb.ELB;
import core.aws.resource.elb.v2.TargetGroup;
import core.aws.resource.vpc.Subnet;
import core.aws.task.as.CreateASGroupTask;
import core.aws.task.as.DeleteASGroupTask;
import core.aws.task.as.DeployASGroupTask;
import core.aws.task.as.DescribeASGroupTask;
import core.aws.task.as.RunCommandTask;
import core.aws.task.as.StartASGroupTask;
import core.aws.task.as.StopASGroupTask;
import core.aws.task.as.UpdateASGroupTask;
import core.aws.task.as.UploadTask;
import core.aws.util.Asserts;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Tasks;

import java.util.List;

/**
 * @author neo
 */
public class ASGroup extends Resource implements ServerResource {
    public static final String TERMINATE_POLICY_OLDEST_INSTANCE = "OldestInstance";

    public com.amazonaws.services.autoscaling.model.AutoScalingGroup remoteASGroup;
    public LaunchConfig launchConfig = new LaunchConfig();
    public ELB elb;
    public TargetGroup targetGroup;
    public int minSize;
    public int maxSize;
    public int desiredSize;
    public Subnet subnet;

    public ASGroup(String id) {
        super(id);
    }

    @Override
    public void validate(Resources resources) {
        if (elb != null) {
            Asserts.isNull(targetGroup, "ASG should have only ELB or TargetGroup setup.");
        }
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateASGroupTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteASGroupTask(this));
    }

    @Override
    protected void updateTasks(Tasks tasks) {
        if (changed() || launchConfig.changed()) {
            tasks.add(new UpdateASGroupTask(this));
        }
    }

    public boolean changed() {
        if (remoteASGroup.getMinSize() != minSize) return true;
        if (remoteASGroup.getMaxSize() != maxSize) return true;
        if (remoteASGroup.getDesiredCapacity() != desiredSize) return true;

        List<String> terminationPolicies = remoteASGroup.getTerminationPolicies();
        return terminationPolicies.size() != 1
            || !TERMINATE_POLICY_OLDEST_INSTANCE.equals(terminationPolicies.get(0));
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeASGroupTask(this));
    }

    @Override
    public void startTasks(Tasks tasks) {
        if (changed())
            tasks.add(new StartASGroupTask(this));
    }

    @Override
    public void stopTasks(Tasks tasks) {
        if (remoteASGroup.getDesiredCapacity() > 0)
            tasks.add(new StopASGroupTask(this));
    }

    @Override
    public void commandTasks(Tasks tasks) {
        tasks.add(new RunCommandTask(this));
    }

    @Override
    public void uploadTasks(Tasks tasks) {
        tasks.add(new UploadTask(this));
    }

    @Override
    public void deployTasks(Tasks tasks) {
        tasks.add(new DeployASGroupTask(this));
    }

    @Override
    public void provisionTasks(Tasks tasks) {
        //TODO: impl provision for as group even if it's transient?
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(id)
            .add(status)
            .add("min", minSize)
            .add("max", maxSize)
            .add("desired", desiredSize)
            .add("ami", launchConfig.ami)
            .add("instanceType", launchConfig.instanceType)
            .toString();
    }
}
