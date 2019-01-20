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

    @Test
    void assumeRolePolicyDocument() {
        IAM iam = new IAM(new DefaultAWSCredentialsProviderChain(), Regions.US_EAST_1);
        String document = "{\"Statement\": [{\"Effect\": \"Allow\",\"Principal\": {\"Service\": \"eks.amazonaws.com\"},\"Action\": \"sts:AssumeRole\"}]}";
        String policyDocument = iam.assumeRolePolicyDocument(document);
        assertThat(policyDocument).contains("\"Version\":\"2012-10-17\"");
    }
}
