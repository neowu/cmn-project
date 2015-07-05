package core.aws.task.elb;

import com.amazonaws.regions.Regions;

/**
 * @author neo
 */
public class ELBAccessLogBucketPolicyBuilder {
    // refer to http://docs.aws.amazon.com/ElasticLoadBalancing/latest/DeveloperGuide/configure-s3-bucket.html
    public String policyText(Regions region, String bucket) {
        return "{\"Version\": \"2012-10-17\", \"Id\": \"elb-access-log-policy\", "
            + "\"Statement\": [{\"Effect\": \"Allow\", \"Principal\": {\"AWS\": \"" + elbPrinciple(region) + "\"}, "
            + "\"Action\": \"s3:PutObject\", \"Resource\": \"arn:aws:s3:::" + bucket + "/elb/*\"}]}";
    }

    private String elbPrinciple(Regions region) {
        switch (region) {
            case US_EAST_1:
                return "127311923021";
            case US_WEST_1:
                return "027434742980";
            case US_WEST_2:
                return "797873946194";
            case EU_WEST_1:
                return "156460612806";
            case AP_NORTHEAST_1:
                return "582318560864";
            case AP_SOUTHEAST_1:
                return "114774131450";
            case AP_SOUTHEAST_2:
                return "783225319266";
            case SA_EAST_1:
                return "507241528517";
            default:
                throw new IllegalStateException("unknown region, region=" + region);
        }
    }
}
