package core.aws.resource.ec2;

/**
 * http://aws.amazon.com/ec2/instance-types/
 *
 * @author neo
 */
public enum InstanceType {
    // old ones
    T1_MICRO("t1.micro"),

    M1_SMALL("m1.small"),
    M1_MEDIUM("m1.medium"),
    M1_LARGE("m1.large"),

    M3_MEDIUM("m3.medium"),
    M3_LARGE("m3.large"),
    M3_X_LARGE("m3.xlarge"),

    C3_LARGE("c3.large"),
    C3_X_LARGE("c3.xlarge"),

    // burstable performance instances
    T2_MICRO("t2.micro"),
    T2_SMALL("t2.small"),
    T2_MEDIUM("t2.medium"),

    // general purpose
    M4_LARGE("m4.large"),
    M4_X_LARGE("m4.xlarge"),

    // compute optimized
    C4_LARGE("c4.large"),
    C4_X_LARGE("c4.xlarge"),

    // memory optimized
    R3_LARGE("r3.large");

    public final String value;

    InstanceType(String value) {
        this.value = value;
    }

    public static InstanceType parse(String value) {
        for (InstanceType instanceType : InstanceType.values()) {
            if (instanceType.value.equals(value)) return instanceType;
        }
        throw new Error("unknown instance-type, value=" + value);
    }
}
