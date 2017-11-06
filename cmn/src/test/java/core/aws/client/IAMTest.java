package core.aws.client;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author neo
 */
class IAMTest {
    @Test
    void assumeRolePolicy() {
        IAM iam = new IAM(new DefaultAWSCredentialsProviderChain(), Regions.US_EAST_1);
        String document = iam.assumeRolePolicyDocument();
        assertThat(document, CoreMatchers.containsString("\"Service\":\"ec2.amazonaws.com\""));
    }

    @Test
    void assumeRolePolicyWithChinaRegion() {
        IAM iam = new IAM(new DefaultAWSCredentialsProviderChain(), Regions.CN_NORTH_1);
        String document = iam.assumeRolePolicyDocument();
        assertThat(document, CoreMatchers.containsString("\"Service\":\"ec2.amazonaws.com.cn\""));
    }
}
