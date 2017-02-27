package core.aws.client;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author neo
 */
public class IAMTest {
    @Test
    public void assumeRolePolicy() {
        IAM iam = new IAM(new DefaultAWSCredentialsProviderChain(), Regions.US_EAST_1);
        String document = iam.assumeRolePolicyDocument();
        Assert.assertThat(document, CoreMatchers.containsString("\"Service\":\"ec2.amazonaws.com\""));
    }

    @Test
    public void assumeRolePolicyWithChinaRegion() {
        IAM iam = new IAM(new DefaultAWSCredentialsProviderChain(), Regions.CN_NORTH_1);
        String document = iam.assumeRolePolicyDocument();
        Assert.assertThat(document, CoreMatchers.containsString("\"Service\":\"ec2.amazonaws.com.cn\""));
    }
}