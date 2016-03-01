package core.aws.resource.ec2;

/**
 * @author neo
 */
public class EBS {
    public static boolean enableEBSOptimized(String instanceType) {
        return instanceType.startsWith("c4") || instanceType.startsWith("m4");
    }

    public final Integer rootVolumeSize;     // in gigabytes
    // TODO: support IOPS

    public EBS(Integer rootVolumeSize) {
        this.rootVolumeSize = rootVolumeSize;
    }
}
