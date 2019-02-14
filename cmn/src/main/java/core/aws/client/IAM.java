package core.aws.client;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.AttachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.AttachedPolicy;
import com.amazonaws.services.identitymanagement.model.CreateInstanceProfileRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleRequest;
import com.amazonaws.services.identitymanagement.model.CreateRoleResult;
import com.amazonaws.services.identitymanagement.model.DeleteRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.DeleteRoleRequest;
import com.amazonaws.services.identitymanagement.model.DeleteServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.DetachRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyResult;
import com.amazonaws.services.identitymanagement.model.GetRoleRequest;
import com.amazonaws.services.identitymanagement.model.GetRoleResult;
import com.amazonaws.services.identitymanagement.model.GetServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.InstanceProfile;
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListAttachedRolePoliciesResult;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesRequest;
import com.amazonaws.services.identitymanagement.model.ListInstanceProfilesResult;
import com.amazonaws.services.identitymanagement.model.ListRolesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolesResult;
import com.amazonaws.services.identitymanagement.model.ListServerCertificatesRequest;
import com.amazonaws.services.identitymanagement.model.ListServerCertificatesResult;
import com.amazonaws.services.identitymanagement.model.NoSuchEntityException;
import com.amazonaws.services.identitymanagement.model.PutRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.Role;
import com.amazonaws.services.identitymanagement.model.ServerCertificate;
import com.amazonaws.services.identitymanagement.model.ServerCertificateMetadata;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateRequest;
import com.amazonaws.services.identitymanagement.model.UploadServerCertificateResult;
import core.aws.util.Asserts;
import core.aws.util.Encodings;
import core.aws.util.Runner;
import core.aws.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author neo
 */
public class IAM {
    public final AmazonIdentityManagement iam;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Region region;

    public IAM(AWSCredentialsProvider credentials, Regions region) {
        iam = AmazonIdentityManagementClientBuilder.standard().withRegion(region).withCredentials(credentials).build();
        this.region = Region.getRegion(region);
    }

    public ServerCertificate createServerCert(UploadServerCertificateRequest request) {
        logger.info("create server cert, path={}, name={}", request.getPath(), request.getServerCertificateName());
        UploadServerCertificateResult result = iam.uploadServerCertificate(request);
        return new ServerCertificate(result.getServerCertificateMetadata(), request.getCertificateBody())
            .withCertificateChain(request.getCertificateChain());
    }

    public void deleteServerCert(String name) throws Exception {
        logger.info("delete server cert, name={}", name);

        // after delete ELB listener, it may not be visible to IAM immediately
        new Runner<Void>()
            .maxAttempts(3)
            .retryInterval(Duration.ofSeconds(20))
            .retryOn(e -> e instanceof AmazonServiceException && "DeleteConflict".equals(((AmazonServiceException) e).getErrorCode()))
            .run(() -> {
                iam.deleteServerCertificate(new DeleteServerCertificateRequest(name));
                return null;
            });
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

    public InstanceProfile createInstanceProfile(String path, String name) {
        CreateInstanceProfileRequest request = new CreateInstanceProfileRequest()
            .withPath(path)
            .withInstanceProfileName(name);

        logger.info("create instance profile, path={}, name={}", path, name);
        return iam.createInstanceProfile(request).getInstanceProfile();
    }

    public String assumeEC2RolePolicyDocument() {
        String service = Strings.format("ec2.{}", region.getDomain());

        return "{\"Statement\":[{\"Effect\":\"Allow\",\"Principal\":{\"Service\":\"" + service + "\"},\"Action\":\"sts:AssumeRole\"}]}";
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
            String policyJSON = Encodings.decodeURL(result.getPolicyDocument());
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

    public Role createRole(String path, String roleName, String policyJSON, String assumeRolePolicyJSON) {
        logger.info("create role, name={}, path={}", roleName, path);
        CreateRoleResult result = iam.createRole(new CreateRoleRequest()
            .withRoleName(roleName)
            .withPath(path)
            .withAssumeRolePolicyDocument(assumeRolePolicyDocument(assumeRolePolicyJSON)));

        if (Strings.notEmpty(policyJSON)) {
            createRolePolicy(roleName, roleName, policyJSON);
        }

        return result.getRole();
    }

    public Role getRole(String roleName) {
        GetRoleResult result = iam.getRole(new GetRoleRequest().withRoleName(roleName));
        return result.getRole();
    }

    String assumeRolePolicyDocument(String document) {
        String statementDoc = document.substring(1, document.length() - 1);
        return "{\"Version\":\"2012-10-17\"," + statementDoc + "}";
    }

    public void attachRolePolicies(String roleName, List<String> attachedPoliciesARNS) {
        logger.info("attach role policy, role={}, arns={}", roleName, attachedPoliciesARNS);
        attachedPoliciesARNS.forEach(policyARN ->
            iam.attachRolePolicy(new AttachRolePolicyRequest().withRoleName(roleName).withPolicyArn(policyARN)));
    }

    public void deleteRole(String roleName, String path) {
        logger.info("delete role, name={}, path={}", roleName, path);
        iam.deleteRolePolicy(new DeleteRolePolicyRequest().withRoleName(roleName).withPolicyName(roleName));
        iam.deleteRole(new DeleteRoleRequest().withRoleName(roleName));
    }

    public void detachRolePolicies(String roleName, List<String> detachedPolicyARNs) {
        logger.info("detach role policy, name={}, policyARNs={}", roleName, detachedPolicyARNs);
        detachedPolicyARNs.forEach(policyARN ->
            iam.detachRolePolicy(new DetachRolePolicyRequest().withRoleName(roleName).withPolicyArn(policyARN)));
    }

    public List<Role> listRoles(String path) {
        logger.info("list roles, path={}", path);
        ListRolesResult result = iam.listRoles(new ListRolesRequest().withPathPrefix(path).withMaxItems(1000));
        Asserts.isFalse(result.isTruncated(), "result is truncated, update to support more roles");
        return result.getRoles();
    }

    public List<String> listAttachedRolePolicyARNs(String roleName) {
        logger.info("list attached role policy arns, roleName={}", roleName);
        ListAttachedRolePoliciesResult result = iam.listAttachedRolePolicies(new ListAttachedRolePoliciesRequest().withRoleName(roleName).withMaxItems(1000));
        Asserts.isFalse(result.isTruncated(), "result is truncated, update to support more attached policies");
        return result.getAttachedPolicies().stream().map(AttachedPolicy::getPolicyArn).collect(Collectors.toList());
    }
}