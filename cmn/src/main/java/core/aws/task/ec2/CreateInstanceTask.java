package core.aws.task.ec2;

import com.amazonaws.services.ec2.model.BlockDeviceMapping;
import com.amazonaws.services.ec2.model.EbsBlockDevice;
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.env.Environment;
import core.aws.resource.ec2.EBS;
import core.aws.resource.ec2.Instance;
import core.aws.resource.vpc.SubnetType;
import core.aws.util.Strings;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.apache.commons.codec.binary.Base64;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author neo
 */
@Action("create-instance")
public class CreateInstanceTask extends Task<Instance> {
    private final int addedCount;
    private final boolean waitUntilInService;

    public CreateInstanceTask(Instance instance, int addedCount, boolean waitUntilInService) {
        super(instance);
        this.addedCount = addedCount;
        this.waitUntilInService = waitUntilInService;
    }

    @Override
    public void execute(Context context) throws Exception {
        String sgId = resource.securityGroup.remoteSecurityGroup.getGroupId();

        RunInstancesRequest request = new RunInstancesRequest()
            .withKeyName(resource.keyPair.remoteKeyPair.getKeyName())
            .withInstanceType(resource.instanceType)
            .withImageId(resource.ami.imageId())
            .withSubnetId(resource.subnet.firstRemoteSubnet().getSubnetId())
            .withSecurityGroupIds(sgId)
            .withMinCount(addedCount)
            .withMaxCount(addedCount)
            .withUserData(Base64.encodeBase64String(Strings.bytes(userData(context.env))));

        if (EBS.enableEBSOptimized(resource.instanceType)) {
            request.withEbsOptimized(true);
        }

        if (resource.instanceProfile != null)
            request.withIamInstanceProfile(new IamInstanceProfileSpecification()
                .withName(resource.instanceProfile.remoteInstanceProfile.getInstanceProfileName()));

        if (resource.ebs.rootVolumeSize != null) {
            request.getBlockDeviceMappings().add(new BlockDeviceMapping()
                .withDeviceName("/dev/sda1")
                .withEbs(new EbsBlockDevice().withVolumeSize(resource.ebs.rootVolumeSize)));
        }

        List<com.amazonaws.services.ec2.model.Instance> remoteInstances = AWS.ec2.runInstances(request, tags(context.env));
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
            AWS.elb.attachInstances(resource.elb.remoteELB.getLoadBalancerName(), instanceIds, waitUntilInService);
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
            .add("addedCount", addedCount)
            .toString();
    }
}
