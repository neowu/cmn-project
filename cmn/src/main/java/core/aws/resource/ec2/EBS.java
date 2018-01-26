package core.aws.resource.ec2;

/**
 * @author neo
 */
public class EBS {
    public static boolean enableEBSOptimized(String instanceType) {
        return instanceType.startsWith("c4") || instanceType.startsWith("m4")
            || instanceType.startsWith("c5") || instanceType.startsWith("m5");
    }

    public Integer rootVolumeSize;     // in gigabytes
    public String type;
}
