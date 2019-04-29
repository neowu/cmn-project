package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest;
import com.amazonaws.services.autoscaling.model.Tag;
import com.amazonaws.services.ec2.model.Subnet;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.as.ASGroup;
import core.aws.util.Lists;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.List;

/**
 * @author neo
 */
@Action("create-asg")
public class CreateASGroupTask extends Task<ASGroup> {
    public CreateASGroupTask(ASGroup asGroup) {
        super(asGroup);
    }

    @Override
    public void execute(Context context) throws Exception {
        ASGroupHelper helper = new ASGroupHelper(context.env);
        helper.createLaunchConfig(resource);

        CreateAutoScalingGroupRequest request = new CreateAutoScalingGroupRequest()
            .withAutoScalingGroupName(context.env.name + "-" + resource.id)
            .withLaunchConfigurationName(resource.launchConfig.remoteLaunchConfig.getLaunchConfigurationName())
            .withDesiredCapacity(resource.desiredSize)
            .withMinSize(resource.minSize)
            .withMaxSize(resource.maxSize)
            .withDefaultCooldown(60)
            .withHealthCheckGracePeriod(300)    // give 5 mins for server and application startup
            .withTerminationPolicies(ASGroup.TERMINATE_POLICY_OLDEST_INSTANCE)      // always remove oldest instance, OldestLaunchConfiguration should not be used due to during deployment the old LaunchConfig can be deleted first, the ASG may fail to compare, and terminate unwanted instance
            .withTags(new Tag().withKey("cloud-manager:env").withValue(context.env.name).withPropagateAtLaunch(Boolean.TRUE),
                helper.nameTag(resource));

        if (resource.elb != null) {
            request.withHealthCheckType("ELB")
                .withLoadBalancerNames(resource.elb.remoteELB.getLoadBalancerName());
        } else if (resource.targetGroup != null) {
            request.withHealthCheckType("ELB")
                .withTargetGroupARNs(resource.targetGroup.remoteTG.getTargetGroupArn());
        } else {
            request.withHealthCheckType("EC2");
        }

        List<String> availabilityZones = Lists.newArrayList();
        StringBuilder subnetIds = new StringBuilder();
        int index = 0;
        for (Subnet remoteSubnet : resource.subnet.remoteSubnets) {
            if (index > 0) subnetIds.append(',');
            subnetIds.append(remoteSubnet.getSubnetId());
            availabilityZones.add(remoteSubnet.getAvailabilityZone());
            index++;
        }
        request.withAvailabilityZones(availabilityZones)
            .withVPCZoneIdentifier(subnetIds.toString());

        resource.remoteASGroup = AWS.getAs().createASGroup(request);
    }
}
