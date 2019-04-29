package core.aws.task.vpc;

import com.amazonaws.services.ec2.model.AssociateRouteTableRequest;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.ModifySubnetAttributeRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.vpc.Subnet;
import core.aws.resource.vpc.SubnetType;
import core.aws.task.ec2.EC2TagHelper;
import core.aws.util.Threads;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * @author neo
 */
@Action("create-subnet")
public class CreateSubnetTask extends Task<Subnet> {
    private final Logger logger = LoggerFactory.getLogger(CreateSubnetTask.class);

    public CreateSubnetTask(Subnet subnet) {
        super(subnet);
    }

    @Override
    public void execute(Context context) throws Exception {
        List<String> zones = AWS.getEc2().availabilityZones();

        List<String> subnetIds = new ArrayList<>();

        for (int i = 0; i < resource.cidrs.size(); i++) {
            String cidr = resource.cidrs.get(i);
            com.amazonaws.services.ec2.model.Subnet subnet = AWS.getVpc().createSubnet(new CreateSubnetRequest(resource.vpc.remoteVPC.getVpcId(), cidr)
                .withAvailabilityZone(zones.get(i)));
            subnetIds.add(subnet.getSubnetId());
        }

        while (true) {
            logger.info("wait until all subnets are available");
            Threads.sleepRoughly(Duration.ofSeconds(10));
            List<com.amazonaws.services.ec2.model.Subnet> subnets = AWS.getVpc().describeSubnets(subnetIds);
            boolean allOK = subnets.stream().allMatch(subnet -> {
                logger.info("subnet {} => {}", subnet.getSubnetId(), subnet.getState());
                return "available".equals(subnet.getState());
            });
            if (allOK) {
                resource.remoteSubnets.addAll(subnets);
                break;
            }
        }

        if (resource.type == SubnetType.PUBLIC) {
            for (String subnetId : subnetIds) {
                AWS.getVpc().ec2.modifySubnetAttribute(new ModifySubnetAttributeRequest()
                    .withSubnetId(subnetId)
                    .withMapPublicIpOnLaunch(Boolean.TRUE));
            }
        }

        EC2TagHelper tagHelper = new EC2TagHelper(context.env);

        AWS.getEc2().createTags(new CreateTagsRequest()
            .withResources(subnetIds)
            .withTags(tagHelper.env(), tagHelper.resourceId(resource.id), tagHelper.name(resource.id)));

        logger.info("associate route table, subnet={}, routeTable={}", resource.id, resource.routeTable.id);
        for (com.amazonaws.services.ec2.model.Subnet remoteSubnet : resource.remoteSubnets) {
            AWS.getVpc().ec2.associateRouteTable(new AssociateRouteTableRequest()
                .withRouteTableId(resource.routeTable.remoteRouteTable.getRouteTableId())
                .withSubnetId(remoteSubnet.getSubnetId()));
        }
    }
}
