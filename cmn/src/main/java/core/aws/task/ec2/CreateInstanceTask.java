package core.aws.task.ec2;

import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.Tag;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.env.Environment;
import core.aws.resource.ec2.EBS;
import core.aws.resource.ec2.Instance;
import core.aws.resource.vpc.SubnetType;
import core.aws.util.Maps;
import core.aws.util.Strings;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.apache.commons.codec.binary.Base64;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author neo
 */
@Action("create-instance")
public class CreateInstanceTask extends Task<Instance> {
    private final int count;
    private final boolean waitUntilInService;

    public CreateInstanceTask(Instance instance, int count, boolean waitUntilInService) {
        super(instance);
        this.count = count;
        this.waitUntilInService = waitUntilInService;
    }

    @Override
    public void execute(Context context) throws Exception {
        Map<String, Integer> addedInstanceCount = planAddedCountBySubnet();

        for (Map.Entry<String, Integer> entry : addedInstanceCount.entrySet()) {
            int count = entry.getValue();
            if (count > 0) {
                String subnetId = entry.getKey();
                createInstance(context, count, subnetId);
            }
        }
    }

    private Map<String, Integer> planAddedCountBySubnet() {
        Map<String, Integer> instanceCount = Maps.newHashMap();
        for (Subnet remoteSubnet : resource.subnet.remoteSubnets) {
            instanceCount.put(remoteSubnet.getSubnetId(), 0);
        }
        for (com.amazonaws.services.ec2.model.Instance remoteInstance : resource.remoteInstances) {
            instanceCount.compute(remoteInstance.getSubnetId(), (key, oldValue) -> oldValue + 1);
        }

        Map<String, Integer> addedInstanceCount = Maps.newHashMap();
        for (String subnetId : instanceCount.keySet()) {
            addedInstanceCount.put(subnetId, 0);
        }

        for (int i = 0; i < count; i++) {
            String targetSubnet = findSubnetHasMinimalInstances(instanceCount);
            instanceCount.compute(targetSubnet, (key, oldValue) -> oldValue + 1);
            addedInstanceCount.compute(targetSubnet, (key, oldValue) -> oldValue + 1);
        }
        return addedInstanceCount;
    }

    private String findSubnetHasMinimalInstances(Map<String, Integer> instanceCount) {
        String subnetId = null;
        int minCount = Integer.MAX_VALUE;
        for (Map.Entry<String, Integer> entry : instanceCount.entrySet()) {
            int count = entry.getValue();
            if (count < minCount) {
                minCount = count;
                subnetId = entry.getKey();
            }
        }
        return subnetId;
    }

    private void createInstance(Context context, int count, String subnetId) throws Exception {
        String sgId = resource.securityGroup.remoteSecurityGroup.getGroupId();

        RunInstancesRequest request = new RunInstancesRequest()
            .withKeyName(resource.keyPair.remoteKeyPair.getKeyName())
            .withInstanceType(resource.instanceType)
            .withImageId(resource.ami.imageId())
            .withSubnetId(subnetId)
            .withSecurityGroupIds(sgId)
            .withMinCount(count)
            .withMaxCount(count)
            .withUserData(Base64.encodeBase64String(Strings.bytes(userData(context.env))));

        if (EBS.enableEBSOptimized(resource.instanceType)) {
            request.withEbsOptimized(Boolean.TRUE);
        }

        if (resource.instanceProfile != null)
            request.withIamInstanceProfile(new IamInstanceProfileSpecification()
                .withName(resource.instanceProfile.remoteInstanceProfile.getInstanceProfileName()));

        if (resource.ebs.rootVolumeSize != null) {
            request.getBlockDeviceMappings().add(new BlockDeviceMapping()
                .withDeviceName("/dev/sda1")
                .withEbs(new EbsBlockDevice().withVolumeSize(resource.ebs.rootVolumeSize).withVolumeType(resource.ebs.type)));
        }

        List<com.amazonaws.services.ec2.model.Instance> remoteInstances = AWS.getEc2().runInstances(request, tags(context.env));
        resource.remoteInstances.addAll(remoteInstances);

        for (com.amazonaws.services.ec2.model.Instance remoteInstance : remoteInstances) {
            String key = String.format("instance/%s/%s", resource.id, remoteInstance.getInstanceId());
            StringBuilder builder = new StringBuilder();
            builder.append("privateIP=").append(remoteInstance.getPrivateIpAddress());
            if (resource.subnet == null || resource.subnet.type == SubnetType.PUBLIC) {
                builder.append(", publicDNS=").append(remoteInstance.getPublicDnsName());
            }
            context.output(key, builder.toString());
        }

        if (resource.elb != null) {
            List<String> instanceIds = remoteInstances.stream().map(com.amazonaws.services.ec2.model.Instance::getInstanceId).collect(Collectors.toList());
            AWS.getElb().attachInstances(resource.elb.remoteELB.getLoadBalancerName(), instanceIds, waitUntilInService);
        }
    }

    private Tag[] tags(Environment env) {
        EC2TagHelper tagHelper = new EC2TagHelper(env);

        String name = name(env);

        return new Tag[]{tagHelper.env(), new Tag("Name", name), tagHelper.resourceId(resource.id)};
    }

    private String name(Environment env) {
        StringBuilder name = new StringBuilder().append(env.name).append(':').append(resource.id);
        if (!resource.id.equals(resource.ami.id())) {
            name.append(':').append(resource.ami.id());
        }

        resource.ami.version()
                    .ifPresent(version -> name.append(":v").append(version));

        return name.toString();
    }

    private String userData(Environment env) {
        return "env=" + env.name + '&'
            + "id=" + resource.id + '&'
            + "name=" + name(env);
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource)
            .add("count", count)
            .toString();
    }
}
