package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.Instance;
import com.amazonaws.services.autoscaling.model.UpdateAutoScalingGroupRequest;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.elasticloadbalancing.model.InstanceState;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.env.Environment;
import core.aws.resource.as.ASGroup;
import core.aws.util.Threads;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * ASG deployment process is not deterministic, during deployment, ASG may update instances and Scaling Policy may kick in
 * <p>
 * The process of deployment:
 * 1) lift desired cap to target cap + step size (the batch size to replace old instances)
 * 2) wait all instances in ASG ready
 * 3) get all current old instances within ASG (current state)
 * 4) if there is enough new instance, break, otherwise terminate some old instances and go back to 2)
 * 5) update ASG with target cap
 *
 * @author neo
 */
@Action("deploy-asg")
public class DeployASGroupTask extends Task<ASGroup> {
    private final Logger logger = LoggerFactory.getLogger(DeployASGroupTask.class);

    String oldLaunchConfigName;
    String asGroupName;
    Set<String> oldInstanceIds;

    public DeployASGroupTask(ASGroup resource) {
        super(resource);
    }

    @Override
    public void execute(Context context) throws Exception {
        if (resource.desiredSize == 0) {
            logger.info("deployment is not needed");
            return;
        }

        oldInstanceIds = listOldInstances();

        Threads.sleepRoughly(Duration.ofSeconds(5));    // shuffle asg deployment, to avoid request rate limitation of AWS ASG

        updateLaunchConfigIfChanged(context.env);

        int capacityDuringDeployment = capacityDuringDeployment(resource.remoteASGroup.getDesiredCapacity(), resource.desiredSize);
        int targetDesiredSize = Math.max(resource.desiredSize, resource.remoteASGroup.getDesiredCapacity());
        int targetMaxSize = Math.max(resource.maxSize, resource.remoteASGroup.getMaxSize());
        logger.info("target desired size => {}, target max size => {}, cap during deployment => {}", targetDesiredSize, targetMaxSize, capacityDuringDeployment);

        context.output("autoScaling/" + resource.id, String.format("targetDesiredSize=%d, oldInstances=%d",
            targetDesiredSize, oldInstanceIds.size()));

        AWS.getAs().updateASGroup(new UpdateAutoScalingGroupRequest()
            .withAutoScalingGroupName(asGroupName)
            .withLaunchConfigurationName(resource.launchConfig.remoteLaunchConfig.getLaunchConfigurationName())
            .withMinSize(capacityDuringDeployment)
            .withDesiredCapacity(capacityDuringDeployment)
            .withMaxSize(Math.max(resource.maxSize, capacityDuringDeployment)));

        if (oldLaunchConfigName != null) AWS.getAs().deleteLaunchConfig(oldLaunchConfigName);

        while (true) {
            waitUntilAllNewInstancesReady();

            com.amazonaws.services.autoscaling.model.AutoScalingGroup remoteASGroup = AWS.getAs().describeASGroup(asGroupName);

            List<String> leftOldInstanceIds = remoteASGroup.getInstances().stream()
                .filter(instance -> oldInstanceIds.contains(instance.getInstanceId()))
                .map(Instance::getInstanceId)
                .collect(Collectors.toList());
            logger.info("left old instances => {}", leftOldInstanceIds);

            if (leftOldInstanceIds.size() <= capacityDuringDeployment - targetDesiredSize) {
                logger.info("created enough new instances, terminate left old instances, finishing deployment");
                AWS.getAs().updateASGroup(new UpdateAutoScalingGroupRequest()
                    .withAutoScalingGroupName(asGroupName)
                    .withMinSize(capacityDuringDeployment - leftOldInstanceIds.size()));
                AWS.getAs().terminateInstancesInASGroup(leftOldInstanceIds, true);
                break;
            } else {
                int terminateCount = targetDesiredSize - (capacityDuringDeployment - leftOldInstanceIds.size());  // how many new instances requires to reach target desired size
                terminateCount = Math.min(terminateCount, leftOldInstanceIds.size());   // check currently has enough old instances
                terminateCount = Math.min(terminateCount, 3);   // terminate with 3 at most
                AWS.getAs().terminateInstancesInASGroup(leftOldInstanceIds.subList(0, terminateCount), false);
            }
        }

        AWS.getAs().updateASGroup(new UpdateAutoScalingGroupRequest()
            .withAutoScalingGroupName(asGroupName)
            .withMinSize(resource.minSize)
            .withDesiredCapacity(targetDesiredSize)
            .withMaxSize(targetMaxSize));
    }

    private Set<String> listOldInstances() {
        asGroupName = resource.remoteASGroup.getAutoScalingGroupName();
        List<Instance> oldInstances = resource.remoteASGroup.getInstances();
        Set<String> oldInstanceIds = oldInstances.stream().map(Instance::getInstanceId).collect(Collectors.toSet());
        logger.info("old instances => {}", oldInstanceIds);
        return oldInstanceIds;
    }

    private void waitUntilAllNewInstancesReady() {
        List<String> newInstanceIds = waitUntilInstanceAttachedToASG();

        waitUntilInstanceRunning(newInstanceIds);

        waitUntilInstanceAttachedToELB(newInstanceIds);
    }

    private List<String> waitUntilInstanceAttachedToASG() {
        List<String> newInstanceIds;
        while (true) {
            Threads.sleepRoughly(Duration.ofSeconds(30));
            com.amazonaws.services.autoscaling.model.AutoScalingGroup remoteASGroup = AWS.getAs().describeASGroup(asGroupName);

            for (Instance instance : remoteASGroup.getInstances()) {
                logger.info("instance status, instanceId={}, lifecycle={}, health={}, new={}",
                    instance.getInstanceId(),
                    instance.getLifecycleState(),
                    instance.getHealthStatus(),
                    !oldInstanceIds.contains(instance.getInstanceId()));
            }

            long inServiceCount = remoteASGroup.getInstances().stream().filter(instance -> "InService".equals(instance.getLifecycleState())).count();

            if (inServiceCount < remoteASGroup.getDesiredCapacity()) {
                logger.info("continue to wait, not all instances are ready, inService={}, desired={}", inServiceCount, remoteASGroup.getDesiredCapacity());
            } else {
                logger.info("all instances of auto scaling group are in service");
                newInstanceIds = remoteASGroup.getInstances().stream()
                    .filter(instance -> !oldInstanceIds.contains(instance.getInstanceId()))
                    .map(Instance::getInstanceId)
                    .collect(Collectors.toList());
                break;
            }
        }
        return newInstanceIds;
    }

    private void waitUntilInstanceRunning(List<String> newInstanceIds) {
        while (true) {
            Threads.sleepRoughly(Duration.ofSeconds(30));
            List<InstanceStatus> statuses = AWS.getEc2().ec2.describeInstanceStatus(new DescribeInstanceStatusRequest()
                .withInstanceIds(newInstanceIds)).getInstanceStatuses();

            for (InstanceStatus status : statuses) {
                logger.info("instance status {} => {}, checks => {}, {}",
                    status.getInstanceId(),
                    status.getInstanceState().getName(),
                    status.getSystemStatus().getStatus(),
                    status.getInstanceStatus().getStatus());
            }

            boolean allOK = statuses.stream().allMatch(status ->
                    "running".equalsIgnoreCase(status.getInstanceState().getName())
                        && "ok".equalsIgnoreCase(status.getSystemStatus().getStatus())
                        && "ok".equalsIgnoreCase(status.getInstanceStatus().getStatus())
            );

            if (allOK) {
                logger.info("all new instances are running");
                break;
            } else {
                logger.info("continue to wait, not all new instances are running");
            }
        }
    }

    private void waitUntilInstanceAttachedToELB(List<String> newInstanceIds) {
        if (resource.elb != null) {
            int attempts = 0;
            while (true) {
                attempts++;
                Threads.sleepRoughly(Duration.ofSeconds(15));

                List<InstanceState> states = AWS.getElb().describeInstanceHealth(resource.elb.remoteELB.getLoadBalancerName(), newInstanceIds);

                for (InstanceState state : states) {
                    logger.info("ELB instance state {} => {}", state.getInstanceId(), state.getState());
                }

                boolean allInService = states.stream().allMatch(state -> "InService".equalsIgnoreCase(state.getState()));
                if (allInService) {
                    logger.info("all new instances of ELB are in service");
                    break;
                } else if (attempts >= 30) {
                    // roughly wait 30*15=450 seconds, where ASG health check grace period is 300 seconds
                    logger.warn("failed to wait all instances to be attached to ELB, it could be instance failed health check after grace period, ASG probably already created new instances to replace, please check AWS console for more details");
                    break;
                } else {
                    logger.info("continue to wait, not all new instances are in service");
                }
            }
        }
    }

    private void updateLaunchConfigIfChanged(Environment env) throws Exception {
        if (resource.launchConfig.changed()) {
            oldLaunchConfigName = resource.remoteASGroup.getLaunchConfigurationName();
            ASGroupHelper helper = new ASGroupHelper(env);
            helper.createLaunchConfig(resource);

            AWS.getAs().updateTag(asGroupName, helper.nameTag(resource));
        }
    }

    int capacityDuringDeployment(int currentDesiredCapacity, int localDesiredCapacity) {
        if (localDesiredCapacity > currentDesiredCapacity)
            return localDesiredCapacity + (currentDesiredCapacity >= 3 ? 3 : currentDesiredCapacity);

        return currentDesiredCapacity >= 3 ? currentDesiredCapacity + 3 : currentDesiredCapacity * 2;
    }
}
