package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.BlockDeviceMapping;
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest;
import com.amazonaws.services.autoscaling.model.Ebs;
import com.amazonaws.services.autoscaling.model.Tag;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.as.AutoScalingGroup;
import core.aws.resource.as.LaunchConfig;
import core.aws.resource.vpc.SubnetType;
import core.aws.util.Randoms;
import core.aws.util.Strings;
import org.apache.commons.codec.binary.Base64;

/**
 * @author neo
 */
public class ASGroupHelper {
    private final Environment env;

    public ASGroupHelper(Environment env) {
        this.env = env;
    }

    public void createLaunchConfig(AutoScalingGroup asGroup) {
        String launchConfigName = env.name + "-" + asGroup.id + "-" + Randoms.alphaNumeric(6);

        LaunchConfig launchConfig = asGroup.launchConfig;

        CreateLaunchConfigurationRequest request = new CreateLaunchConfigurationRequest()
            .withLaunchConfigurationName(launchConfigName)
            .withKeyName(launchConfig.keyPair.remoteKeyPair.getKeyName())
            .withInstanceType(launchConfig.instanceType)
            .withImageId(launchConfig.ami.imageId())
            .withSecurityGroups(launchConfig.securityGroup.remoteSecurityGroup.getGroupId())
            .withUserData(Base64.encodeBase64String(Strings.bytes(userData(asGroup))));

        if (asGroup.subnet.type == SubnetType.PUBLIC) {
            request.withAssociatePublicIpAddress(true);
        }

        if (launchConfig.instanceProfile != null)
            request.withIamInstanceProfile(launchConfig.instanceProfile.remoteInstanceProfile.getInstanceProfileName());

        if (launchConfig.ebs.rootVolumeSize != null) {
            request.getBlockDeviceMappings().add(new BlockDeviceMapping()
                .withDeviceName("/dev/sda1")
                .withEbs(new Ebs().withVolumeSize(launchConfig.ebs.rootVolumeSize)));
        }

        launchConfig.remoteLaunchConfig = AWS.as.createLaunchConfig(request);
    }

    private String userData(AutoScalingGroup asGroup) {
        return "env=" + env.name + '&'
            + "id=" + asGroup.id + '&'
            + "name=" + instanceName(asGroup);
    }

    Tag nameTag(AutoScalingGroup asGroup) {
        return new Tag().withKey("Name").withValue(instanceName(asGroup)).withPropagateAtLaunch(true);
    }

    String instanceName(AutoScalingGroup asGroup) {
        StringBuilder name = new StringBuilder().append(env.name).append(':').append(asGroup.id);
        if (!asGroup.id.equals(asGroup.launchConfig.ami.id())) {
            name.append(':').append(asGroup.launchConfig.ami.id());
        }
        asGroup.launchConfig.ami.version()
            .ifPresent(version -> name.append(":v").append(version));
        return name.toString();
    }
}