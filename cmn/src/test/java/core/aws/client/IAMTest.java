package core.aws.client;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.regions.Regions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author neo
 */
class IAMTest {
    @Test
    void assumeRolePolicy() {
        IAM iam = new IAM(new DefaultAWSCredentialsProviderChain(), Regions.US_EAST_1);
        String document = iam.assumeEC2RolePolicyDocument();
        assertThat(document).contains("\"Service\":\"ec2.amazonaws.com\"");
    }

    @Test
    void assumeRolePolicyWithChinaRegion() {
        IAM iam = new IAM(new DefaultAWSCredentialsProviderChain(), Regions.CN_NORTH_1);
        String document = iam.assumeEC2RolePolicyDocument();
        assertThat(document).contains("\"Service\":\"ec2.amazonaws.com.cn\"");
    }
}
