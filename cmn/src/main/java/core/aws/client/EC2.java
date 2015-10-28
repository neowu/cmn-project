package core.aws.client;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.AuthorizeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.AvailabilityZone;
import com.amazonaws.services.ec2.model.CreateKeyPairRequest;
import com.amazonaws.services.ec2.model.CreateKeyPairResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateSecurityGroupResult;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DeleteKeyPairRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DescribeAvailabilityZonesResult;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.DescribeInstanceStatusRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.DescribeTagsResult;
import com.amazonaws.services.ec2.model.Image;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceStatus;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.KeyPair;
import com.amazonaws.services.ec2.model.RevokeSecurityGroupIngressRequest;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.RunInstancesResult;
import com.amazonaws.services.ec2.model.SecurityGroup;
import com.amazonaws.services.ec2.model.StartInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagDescription;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import core.aws.resource.ec2.InstanceState;
import core.aws.util.Asserts;
import core.aws.util.Runner;
import core.aws.util.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author neo
 */
public class EC2 {
    public final AmazonEC2 ec2;
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private volatile List<String> availabilityZones;

    public EC2(AWSCredentialsProvider credentials, Region region) {
        ec2 = new AmazonEC2Client(credentials);
        ec2.setRegion(region);
    }

    public synchronized List<String> availabilityZones() {
        if (availabilityZones == null) {
            DescribeAvailabilityZonesResult result = ec2.describeAvailabilityZones();
            availabilityZones = result.getAvailabilityZones().stream()
                .filter(zone -> "available".equals(zone.getState()) && !"ap-northeast-1a".equals(zone.getZoneName()))   // AWS bug, japan 1a returned as available but can't be used
                .map(AvailabilityZone::getZoneName)
                .collect(Collectors.toList());
            logger.info("availability zones => {}", availabilityZones);
        }
        return availabilityZones;
    }

    public KeyPair createKeyPair(String keyName) {
        logger.info("create key pair, keyName={}", keyName);
        CreateKeyPairResult result = ec2.createKeyPair(new CreateKeyPairRequest().withKeyName(keyName));
        return result.getKeyPair();
    }

    public boolean keyPairExists(String name) {
        try {
            return !ec2.describeKeyPairs(new DescribeKeyPairsRequest().withKeyNames(name)).getKeyPairs().isEmpty();
        } catch (AmazonServiceException e) {
            if ("InvalidKeyPair.NotFound".equals(e.getErrorCode())) {
                return false;
            }
            throw e;
        }
    }

    public void deleteKeyPair(String keyPairName) {
        logger.info("delete keyPair, keyName={}", keyPairName);
        ec2.deleteKeyPair(new DeleteKeyPairRequest(keyPairName));
    }

    public List<Instance> runInstances(RunInstancesRequest request, Tag... tags) throws Exception {
        logger.info("create ec2 instance, request={}", request);

        RunInstancesResult result = new Runner<RunInstancesResult>()
            .maxAttempts(3)
            .retryInterval(Duration.ofSeconds(20))
            .retryOn(this::retryOnRunInstance)
            .run(() -> ec2.runInstances(request));

        Threads.sleepRoughly(Duration.ofSeconds(5)); // wait little bit to make sure instance is visible to tag service
        List<String> instanceIds = result.getReservation().getInstances().stream().map(Instance::getInstanceId).collect(Collectors.toList());

        CreateTagsRequest tagsRequest = new CreateTagsRequest()
            .withResources(instanceIds)
            .withTags(tags);

        createTags(tagsRequest);

        waitUntilRunning(instanceIds);

        return describeInstances(instanceIds);
    }

    private boolean retryOnRunInstance(Exception e) {
        if (!(e instanceof AmazonServiceException)) {
            return false;
        }
        AmazonServiceException awsException = (AmazonServiceException) e;
        if (awsException.getErrorMessage().contains("iamInstanceProfile.name"))
            return true; // iam may not be visible immediately after creation
        if ("RequestLimitExceeded".equals(awsException.getErrorCode()))
            return true; // retry if request rate limit exceeds, this seems depends on load on AWS side, and may happen if bake many instances same time.
        return false;
    }

    public void stopInstances(List<String> instanceIds) throws InterruptedException {
        if (instanceIds.isEmpty()) throw new Error("instanceIds must not be empty");
        logger.info("stop instances, instanceIds={}", instanceIds);
        ec2.stopInstances(new StopInstancesRequest().withInstanceIds(instanceIds));
        waitUntil(instanceIds, InstanceState.STOPPED);
    }

    public void startInstances(List<String> instanceIds) throws InterruptedException {
        if (instanceIds.isEmpty()) throw new Error("instanceIds must not be empty");
        logger.info("start instances, instanceIds={}", instanceIds);
        ec2.startInstances(new StartInstancesRequest().withInstanceIds(instanceIds));
        waitUntilRunning(instanceIds);
    }

    public void terminateInstances(List<String> instanceIds) throws InterruptedException {
        logger.info("terminate instances, instanceIds={}", instanceIds);
        ec2.terminateInstances(new TerminateInstancesRequest().withInstanceIds(instanceIds));
        waitUntil(instanceIds, InstanceState.TERMINATED);
    }

    public SecurityGroup createSecurityGroup(CreateSecurityGroupRequest request) {
        logger.info("create security group, groupName={}", request.getGroupName());
        SecurityGroup securityGroup = new SecurityGroup();
        CreateSecurityGroupResult result = ec2.createSecurityGroup(request);
        securityGroup.setGroupName(request.getGroupName());
        securityGroup.setGroupId(result.getGroupId());
        return securityGroup;
    }

    public void deleteSGIngressRules(String securityGroupId, List<IpPermission> rules) {
        logger.info("delete ingress sg rules, sgId={}, rules={}", securityGroupId, rules);

        ec2.revokeSecurityGroupIngress(new RevokeSecurityGroupIngressRequest()
            .withGroupId(securityGroupId)
            .withIpPermissions(rules));
    }

    public void createSGIngressRules(String securityGroupId, List<IpPermission> rules) {
        logger.info("create ingress sg rules, sgId={}, rules={}", securityGroupId, rules);

        ec2.authorizeSecurityGroupIngress(new AuthorizeSecurityGroupIngressRequest()
            .withGroupId(securityGroupId)
            .withIpPermissions(rules));
    }

    public void deleteSecurityGroup(String securityGroupId) {
        logger.info("delete security group, securityGroupId={}", securityGroupId);
        ec2.deleteSecurityGroup(new DeleteSecurityGroupRequest().withGroupId(securityGroupId));
    }

    public void createTags(final CreateTagsRequest request) throws Exception {
        new Runner<>()
            .retryInterval(Duration.ofSeconds(5))
            .maxAttempts(3)
            .retryOn(e -> e instanceof AmazonServiceException)
            .run(() -> {
                logger.info("create tags, request={}", request);
                ec2.createTags(request);
                return null;
            });
    }

    public List<TagDescription> describeTags(DescribeTagsRequest request) {
        logger.info("describe tags, request={}", request);
        DescribeTagsResult result = ec2.describeTags(request);
        Asserts.isNull(result.getNextToken(), "tags pagination is not supported yet, token={}", result.getNextToken());
        return result.getTags();
    }

    public List<Instance> describeInstances(Collection<String> instanceIds) {
        if (instanceIds.isEmpty())
            throw new IllegalArgumentException("instanceIds can not be empty, otherwise it requires all instances");
        logger.info("describe instances, instanceIds={}", instanceIds);
        DescribeInstancesResult result = ec2.describeInstances(new DescribeInstancesRequest().withInstanceIds(instanceIds));
        return result.getReservations().stream()
            .flatMap(reservation -> reservation.getInstances().stream())
            .collect(Collectors.toList());
    }

    public List<Image> describeImages(Collection<String> imageIds) {
        if (imageIds.isEmpty())
            throw new IllegalArgumentException("imageIds can not be empty, otherwise it requires all images");
        logger.info("describe images, imageIds={}", imageIds);
        DescribeImagesResult result = ec2.describeImages(new DescribeImagesRequest().withImageIds(imageIds));
        return result.getImages();
    }

    public void waitUntilRunning(List<String> instanceIds) throws InterruptedException {
        int attempts = 0;
        while (true) {
            attempts++;
            Threads.sleepRoughly(Duration.ofSeconds(30));
            List<InstanceStatus> statuses = ec2.describeInstanceStatus(new DescribeInstanceStatusRequest()
                .withInstanceIds(instanceIds)).getInstanceStatuses();

            if (statuses.size() < instanceIds.size()) {
                logger.info("status is not synced, continue to wait");
                continue;
            }

            for (InstanceStatus status : statuses) {
                logger.info("instance status {} => {}, checks => {}, {}",
                    status.getInstanceId(),
                    status.getInstanceState().getName(),
                    status.getSystemStatus().getStatus(),
                    status.getInstanceStatus().getStatus());
            }

            boolean allOK = statuses.stream().allMatch(status ->
                "running".equalsIgnoreCase(status.getInstanceState().getName())
                    && "ok".equalsIgnoreCase(status.getSystemStatus().getStatus())
                    && "ok".equalsIgnoreCase(status.getInstanceStatus().getStatus()));

            if (allOK) {
                break;
            } else if (attempts > 20) { // roughly after 10 mins
                throw new Error("waited too long to get instance status, something is wrong, please check aws console");
            }
        }
    }

    private void waitUntil(List<String> instanceIds, final InstanceState expectedState) throws InterruptedException {
        while (true) {
            Threads.sleepRoughly(Duration.ofSeconds(20));
            List<Instance> instances = describeInstances(instanceIds);

            for (Instance instance : instances) {
                logger.info("instance status {} => {}", instance.getInstanceId(), instance.getState().getName());
            }

            boolean allOK = instances.stream().allMatch(instance -> expectedState.equalsTo(instance.getState()));

            if (allOK) break;
        }
    }
}
