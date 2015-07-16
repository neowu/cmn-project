package core.aws.task.vpc;

import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceNetworkInterfaceSpecification;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.ModifyInstanceAttributeRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.env.Environment;
import core.aws.resource.vpc.NAT;
import core.aws.task.ec2.EC2TagHelper;
import core.aws.util.Randoms;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("create-nat")
public class CreateNATTask extends Task<NAT> {
    public CreateNATTask(NAT nat) {
        super(nat);
    }

    @Override
    public void execute(Context context) throws Exception {
        String sgId = createSG(context.env);

        Instance instance = createNATInstance(sgId, context.env);
        resource.remoteInstance = instance;
        String natId = instance.getInstanceId();

        AWS.ec2.ec2.modifyInstanceAttribute(new ModifyInstanceAttributeRequest()
            .withInstanceId(natId)
            .withSourceDestCheck(false));
    }

    private Instance createNATInstance(String sgId, Environment env) throws Exception {
        EC2TagHelper tagHelper = new EC2TagHelper(env);

        RunInstancesRequest request = new RunInstancesRequest()
            .withKeyName(resource.keyPair.remoteKeyPair.getKeyName())
            .withInstanceType(InstanceType.M3Medium)
            .withImageId(resource.image.imageId())
            .withMinCount(1)
            .withMaxCount(1);

        request.getNetworkInterfaces().add(new InstanceNetworkInterfaceSpecification()
            .withDeviceIndex(0)
            .withSubnetId(resource.publicSubnet.remoteSubnets.get(0).getSubnetId())
            .withGroups(sgId)
            .withAssociatePublicIpAddress(true));

        return AWS.ec2.runInstances(request, tagHelper.env(), tagHelper.resourceId(resource.id), tagHelper.name(resource.id), tagHelper.type("nat")).get(0);
    }

    private String createSG(Environment env) {
        String sgName = env.name + ":nat:" + Randoms.alphaNumeric(6);
        String sgId = AWS.ec2.createSecurityGroup(new CreateSecurityGroupRequest(sgName, sgName).withVpcId(resource.vpc.remoteVPC.getVpcId())).getGroupId();
        AWS.ec2.ec2.authorizeSecurityGroupIngress(new AuthorizeSecurityGroupIngressRequest()
            .withGroupId(sgId)
            .withIpPermissions(new IpPermission()
                .withIpRanges("10.0.0.0/16")
                .withFromPort(80)
                .withToPort(80)
                .withIpProtocol("tcp")));
        AWS.ec2.ec2.authorizeSecurityGroupIngress(new AuthorizeSecurityGroupIngressRequest()
            .withGroupId(sgId)
            .withIpPermissions(new IpPermission()
                .withIpRanges("10.0.0.0/16")
                .withFromPort(443)
                .withToPort(443)
                .withIpProtocol("tcp")));
        return sgId;
    }
}
