package core.aws.resource.ec2;

import core.aws.resource.Resource;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.resource.ServerResource;
import core.aws.resource.elb.ELB;
import core.aws.resource.image.AMI;
import core.aws.resource.vpc.Subnet;
import core.aws.task.ec2.CreateInstanceTask;
import core.aws.task.ec2.DeleteInstanceTask;
import core.aws.task.ec2.DescribeInstanceTask;
import core.aws.task.ec2.ProvisionInstanceTask;
import core.aws.task.ec2.RunCommandTask;
import core.aws.task.ec2.StartInstanceTask;
import core.aws.task.ec2.StopInstanceTask;
import core.aws.task.ec2.UploadTask;
import core.aws.util.Asserts;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author neo
 */
public class Instance extends Resource implements ServerResource {
    public final List<com.amazonaws.services.ec2.model.Instance> remoteInstances = new ArrayList<>();
    public EBS ebs;
    public KeyPair keyPair;
    public AMI ami;
    public InstanceType instanceType;
    public SecurityGroup securityGroup;
    public int count = 1;
    public Subnet subnet;
    public ELB elb;
    public InstanceProfile instanceProfile;

    public Instance(String id) {
        super(id);
    }

    @Override
    public void validate(Resources resources) {
        if (status == ResourceStatus.LOCAL_REMOTE && !remoteInstances.isEmpty()) {
            for (com.amazonaws.services.ec2.model.Instance remoteInstance : remoteInstances) {
                Asserts.equals(remoteInstance.getVpcId(), resources.vpc.remoteVPC.getVpcId(), "remote instance is in different vpc");
            }
        }
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeInstanceTask(this));
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
    public void startTasks(Tasks tasks) {
        if (!stoppedInstanceIds().isEmpty()) {
            tasks.add(new StartInstanceTask(this));
        }
    }

    @Override
    public void stopTasks(Tasks tasks) {
        if (!runningInstanceIds().isEmpty()) {
            tasks.add(new StopInstanceTask(this));
        }
    }

    @Override
    public void provisionTasks(Tasks tasks) {
        tasks.add(new ProvisionInstanceTask(this));
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateInstanceTask(this, count));
    }

    @Override
    public void deployTasks(Tasks tasks) {
        updateTasks(tasks, true);
    }

    @Override
    protected void updateTasks(Tasks tasks) {
        updateTasks(tasks, false);
    }

    private void updateTasks(Tasks tasks, boolean isDeployment) {
        CreateInstanceTask createInstanceTask = null;
        DeleteInstanceTask deleteInstanceTask = null;

        List<com.amazonaws.services.ec2.model.Instance> deletedInstances = remoteInstances.stream()
            .filter(this::changed).collect(Collectors.toList());
        remoteInstances.removeAll(deletedInstances);

        if (count > remoteInstances.size()) {
            createInstanceTask = tasks.add(new CreateInstanceTask(this, count - remoteInstances.size()));
        } else if (count < remoteInstances.size()) {
            List<com.amazonaws.services.ec2.model.Instance> redundantInstances = new ArrayList<>(remoteInstances.subList(0, remoteInstances.size() - count));
            remoteInstances.removeAll(redundantInstances);
            deletedInstances.addAll(redundantInstances);
        }

        if (!deletedInstances.isEmpty())
            deleteInstanceTask = tasks.add(new DeleteInstanceTask(this, deletedInstances));

        if (isDeployment && createInstanceTask != null && deleteInstanceTask != null) {
            deleteInstanceTask.dependsOn(createInstanceTask);
        }
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteInstanceTask(this, remoteInstances));
    }

    private boolean changed(com.amazonaws.services.ec2.model.Instance instance) {
        if (!instance.getInstanceType().equals(instanceType.value)) return true;
        if (!instance.getImageId().equals(ami.imageId())) return true;
        // check if instance profile added or removed
        boolean localHasInstanceProfile = instanceProfile != null;
        boolean remoteHasInstanceProfile = instance.getIamInstanceProfile() != null;
        if (localHasInstanceProfile != remoteHasInstanceProfile) return true;
        return false;
    }

    public List<String> stoppedInstanceIds() {
        return remoteInstances.stream()
            .filter(remoteInstance -> InstanceState.STOPPED.equalsTo(remoteInstance.getState()))
            .map(com.amazonaws.services.ec2.model.Instance::getInstanceId)
            .collect(Collectors.toList());
    }

    public List<String> runningInstanceIds() {
        return remoteInstances.stream()
            .filter(remoteInstance -> InstanceState.RUNNING.equalsTo(remoteInstance.getState()))
            .map(com.amazonaws.services.ec2.model.Instance::getInstanceId)
            .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(id)
            .add(status)
            .add("ami", ami)
            .add("instanceType", instanceType)
            .add("count", count)
            .add("subnet", subnet)
            .addIfNotNull("instanceProfile", instanceProfile)
            .addIfNotNull("elb", elb)
            .toString();
    }
}
