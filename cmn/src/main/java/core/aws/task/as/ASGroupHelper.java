package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.BlockDeviceMapping;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.Ebs;
import com.amazonaws.services.autoscaling.model.Tag;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.as.ASGroup;
import core.aws.resource.as.LaunchConfig;
import core.aws.resource.ec2.EBS;
import core.aws.resource.vpc.SubnetType;
import core.aws.util.Encodings;
import core.aws.util.Randoms;

/**
 * @author neo
 */
public class ASGroupHelper {
    private final Environment env;

    public ASGroupHelper(Environment env) {
        this.env = env;
    }

    public void createLaunchConfig(ASGroup asGroup) throws Exception {
        String launchConfigName = env.name + "-" + asGroup.id + "-" + Randoms.alphaNumeric(6);

        LaunchConfig launchConfig = asGroup.launchConfig;

        CreateLaunchConfigurationRequest request = new CreateLaunchConfigurationRequest()
            .withLaunchConfigurationName(launchConfigName)
            .withKeyName(launchConfig.keyPair.remoteKeyPair.getKeyName())
            .withInstanceType(launchConfig.instanceType)
            .withImageId(launchConfig.ami.imageId())
            .withSecurityGroups(launchConfig.securityGroup.remoteSecurityGroup.getGroupId())
            .withUserData(Encodings.base64(userData(asGroup)));

        if (EBS.enableEBSOptimized(launchConfig.instanceType)) {    // this is not necessary since m4/c4 are EBS optimized enable by default, but there is bug in AWS console, we need to set this in order to display correct value
            request.withEbsOptimized(Boolean.TRUE);
        }

        if (asGroup.subnet.type == SubnetType.PUBLIC) {
            request.withAssociatePublicIpAddress(Boolean.TRUE);
        }

        if (launchConfig.instanceProfile != null)
            request.withIamInstanceProfile(launchConfig.instanceProfile.remoteInstanceProfile.getInstanceProfileName());

        if (launchConfig.ebs.rootVolumeSize != null) {
            request.getBlockDeviceMappings().add(new BlockDeviceMapping()
                .withDeviceName("/dev/sda1")
                .withEbs(new Ebs().withVolumeSize(launchConfig.ebs.rootVolumeSize).withVolumeType(launchConfig.ebs.type)));
        }

        launchConfig.remoteLaunchConfig = AWS.getAs().createLaunchConfig(request);
    }

    private String userData(ASGroup asGroup) {
        return "env=" + env.name + '&'
            + "id=" + asGroup.id + '&'
            + "name=" + instanceName(asGroup);
    }

    Tag nameTag(ASGroup asGroup) {
        return new Tag().withKey("Name").withValue(instanceName(asGroup)).withPropagateAtLaunch(Boolean.TRUE);
    }

    String instanceName(ASGroup asGroup) {
        StringBuilder name = new StringBuilder().append(env.name).append(':').append(asGroup.id);
        if (!asGroup.id.equals(asGroup.launchConfig.ami.id())) {
            name.append(':').append(asGroup.launchConfig.ami.id());
        }
        asGroup.launchConfig.ami.version()
            .ifPresent(version -> name.append(":v").append(version));
        return name.toString();
    }
}
