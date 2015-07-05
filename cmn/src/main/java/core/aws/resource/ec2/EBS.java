package core.aws.resource.ec2;

/**
 * @author neo
 */
public class EBS {
    public final Integer rootVolumeSize;     // in gigabytes
    // TODO: support IOPS

    public EBS(Integer rootVolumeSize) {
        this.rootVolumeSize = rootVolumeSize;
    }
}
