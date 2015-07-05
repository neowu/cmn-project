package core.aws.client;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.regions.Region;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.AddRoleToInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.DeleteServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyResult;
import com.amazonaws.services.identitymanagement.model.GetServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.identitymanagement.model.ListServerCertificatesRequest;
import com.amazonaws.services.identitymanagement.model.ListServerCertificatesResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.ServerCertificate;
import com.amazonaws.services.identitymanagement.model.ServerCertificateMetadata;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateResult;
import core.aws.util.Asserts;
import core.aws.util.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
public class IAM {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public final AmazonIdentityManagement iam;
    private final Region region;

    public IAM(AWSCredentialsProvider credentials, Region region) {
        iam = new AmazonIdentityManagementClient(credentials);
        iam.setRegion(region);
        this.region = region;
    }

    public ServerCertificate createServerCert(UploadServerCertificateRequest request) throws InterruptedException {
        logger.info("create server cert, path={}, name={}", request.getPath(), request.getServerCertificateName());
        UploadServerCertificateResult result = iam.uploadServerCertificate(request);
        return new ServerCertificate(result.getServerCertificateMetadata(), request.getCertificateBody())
            .withCertificateChain(request.getCertificateChain());
    }

    public void deleteServerCert(String name) {
        logger.info("delete server cert, name={}", name);
        iam.deleteServerCertificate(new DeleteServerCertificateRequest(name));
    }

    public List<ServerCertificateMetadata> listServerCerts(String path) {
        logger.info("list server certs, path={}", path);
        ListServerCertificatesResult result = iam.listServerCertificates(new ListServerCertificatesRequest().withPathPrefix(path));
        return result.getServerCertificateMetadataList();
    }

    public List<InstanceProfile> listInstanceProfiles(String path) {
        logger.info("list instance profiles, path={}", path);
        ListInstanceProfilesResult result = iam.listInstanceProfiles(new ListInstanceProfilesRequest().withPathPrefix(path).withMaxItems(1000));
        Asserts.isFalse(result.isTruncated(), "result is truncated, update to support more instance profiles");
        return result.getInstanceProfiles();
    }

    public InstanceProfile createInstanceProfile(String path, String name, String policy) {
        CreateInstanceProfileRequest request = new CreateInstanceProfileRequest()
            .withPath(path)
            .withInstanceProfileName(name);

        logger.info("create instance profile, path={}, name={}", path, name);
        InstanceProfile instanceProfile = iam.createInstanceProfile(request).getInstanceProfile();

        logger.info("create role, name={}", name);
        iam.createRole(new CreateRoleRequest()
            .withRoleName(name)
            .withPath(path)
            .withAssumeRolePolicyDocument(assumeRolePolicyDocument()));

        // attach role to instance before creating policy, if policy failed, at least profile/role are ready, and policy can be fixed thru AWS console
        iam.addRoleToInstanceProfile(new AddRoleToInstanceProfileRequest()
            .withInstanceProfileName(name)
            .withRoleName(name));

        createRolePolicy(name, name, policy);

        return instanceProfile;
    }

    String assumeRolePolicyDocument() {
        String service = "ec2." + region.getDomain();

        return "{\"Version\":\"2012-10-17\",\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"" + service + "\"},\"Action\":\"sts:AssumeRole\"}]}";
    }

    public ServerCertificate getServerCert(String serverCertName) {
        logger.info("get server cert, name={}", serverCertName);
        return iam.getServerCertificate(new GetServerCertificateRequest(serverCertName)).getServerCertificate();
    }

    public Optional<Policy> findRolePolicy(String roleName, String policyName) {
        logger.info("find role policy, roleName={}, policyName={}", roleName, policyName);
        try {
            GetRolePolicyResult result = iam.getRolePolicy(new GetRolePolicyRequest()
                .withRoleName(roleName)
                .withPolicyName(policyName));
            String policyJSON = EncodingUtils.decodeURL(result.getPolicyDocument());
            return Optional.of(Policy.fromJson(policyJSON));
        } catch (NoSuchEntityException e) {
            return Optional.empty();
        }
    }

    public void createRolePolicy(String roleName, String policyName, String policyJSON) {
        logger.info("create role policy, role={}, policyName={}, policyJSON={}", roleName, policyName, policyJSON);
        iam.putRolePolicy(new PutRolePolicyRequest()
            .withRoleName(roleName)
            .withPolicyName(policyName)
            .withPolicyDocument(policyJSON));
    }
}