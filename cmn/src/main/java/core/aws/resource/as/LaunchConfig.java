package core.aws.resource.as;

import com.amazonaws.services.autoscaling.model.LaunchConfiguration;
import core.aws.resource.ec2.EBS;
import core.aws.resource.ec2.InstanceProfile;
import core.aws.resource.ec2.KeyPair;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.resource.image.AMI;

/**
 * @author neo
 */
public class LaunchConfig {
    public LaunchConfiguration remoteLaunchConfig;
    public EBS ebs;
    public KeyPair keyPair;
    public AMI ami;
    public String instanceType;
    public SecurityGroup securityGroup;
    public InstanceProfile instanceProfile;

    public boolean changed() {
        if (!remoteLaunchConfig.getImageId().equals(ami.imageId())) return true;
        if (!remoteLaunchConfig.getInstanceType().equals(instanceType)) return true;
        return false;
    }
}
