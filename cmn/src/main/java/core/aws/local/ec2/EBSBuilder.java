package core.aws.local.ec2;

import core.aws.resource.ec2.EBS;
import core.aws.util.Asserts;

import java.util.Map;

/**
 * @author neo
 */
public class EBSBuilder {
    public EBS build(Map<String, Object> params) {
        Integer rootVolumeSize = parseSize((String) params.get("size"));
        EBS ebs = new EBS();
        ebs.rootVolumeSize = rootVolumeSize;
        ebs.type = (String) params.get("type");
        return ebs;
    }

    Integer parseSize(String size) {
        if (size == null) return null;
        Asserts.isTrue(size.endsWith("G"), "size only supports format like 30G");
        return Integer.valueOf(size.substring(0, size.length() - 1));
    }
}
