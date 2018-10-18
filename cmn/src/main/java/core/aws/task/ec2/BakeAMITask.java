package core.aws.task.ec2;

import com.amazonaws.services.ec2.model.CreateImageRequest;
import com.amazonaws.services.ec2.model.CreateImageResult;
import com.amazonaws.services.ec2.model.CreateSecurityGroupRequest;
import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.DescribeImagesRequest;
import com.amazonaws.services.ec2.model.DescribeImagesResult;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.IpRange;
import com.amazonaws.services.ec2.model.RunInstancesRequest;
import com.amazonaws.services.ec2.model.Subnet;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.env.Environment;
import core.aws.resource.ec2.Instance;
import core.aws.resource.ec2.InstanceState;
import core.aws.resource.ec2.KeyPair;
import core.aws.resource.image.Image;
import core.aws.task.linux.AnsibleProvisioner;
import core.aws.util.Asserts;
import core.aws.util.Lists;
import core.aws.util.Threads;
import core.aws.workflow.Action;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author neo
 */
@Action("bake")
public class BakeAMITask extends core.aws.workflow.Task<Image> {
    private final Logger logger = LoggerFactory.getLogger(BakeAMITask.class);

    private final Instance resumeBakeInstance;

    private String resourceId;
    private KeyPairHelper keyPairHelper;
    private EC2TagHelper tagHelper;
    private Subnet bakeSubnet;

    public BakeAMITask(Image image, Instance resumeBakeInstance) {
        super(image);
        this.resumeBakeInstance = resumeBakeInstance;
    }

    @Override
    public void execute(Context context) throws Exception {
        tagHelper = new EC2TagHelper(context.env);
        keyPairHelper = new KeyPairHelper(context.env);

        String bakeSubnetId = context.env.bakeSubnetId;
        if (bakeSubnetId != null) {
            bakeSubnet = AWS.getVpc().describeSubnets(Lists.newArrayList(bakeSubnetId)).get(0);
            if (!bakeSubnet.isMapPublicIpOnLaunch())
                throw new Error("bake subnet does not auto assign public ip, please check the subnet setting, subnetId=" + bakeSubnetId);
        }

        resourceId = "ami-" + resource.id + LocalDateTime.now().format(DateTimeFormatter.ofPattern("-yyyyMMdd-HHmm"));

        com.amazonaws.services.ec2.model.Instance instance;

        if (resumeBakeInstance != null) {
            instance = resumeBakeInstance.remoteInstances.get(0);
            logger.info("resume bake with {}, instanceId={}", resumeBakeInstance.id, instance.getInstanceId());
            Asserts.isTrue(InstanceState.RUNNING.equalsTo(instance.getState()), "resume bake instance must be running, instanceId={}, currentState={}", instance.getInstanceId(), instance.getState().getName());
        } else {
            KeyPair keyPair = createKeyPair(context.env);
            String sgId = createSG(context.env);
            instance = createInstance(keyPair, sgId);
        }

        AnsibleProvisioner provisioner = new AnsibleProvisioner(context.env, instance, resource.playbook, resource.packageDir);
        provisioner.additionalVariables.put("tomcat_service_state", "stopped");
        provisioner.additionalVariables.put("supervisor_service_state", "stopped");
        provisioner.additionalVariables.put("nginx_service_state", "stopped");
        provisioner.additionalVariables.put("elasticsearch_heap_size", "1g");
        provisioner.provision();

        String imageId = createAMI(context, instance.getInstanceId());

        AWS.getEc2().terminateInstances(Lists.newArrayList(instance.getInstanceId()));

        keyPairHelper.deleteKeyPair(instance.getKeyName());
        AWS.getEc2().deleteSecurityGroup(instance.getSecurityGroups().get(0).getGroupId());
        waitUntilAMIFinished(imageId);
    }

    private void waitUntilAMIFinished(String imageId) {
        logger.info("wait until AMI finished, imageId={}", imageId);
        while (true) {
            DescribeImagesResult result = AWS.getEc2().ec2.describeImages(new DescribeImagesRequest().withImageIds(imageId));
            String state = result.getImages().get(0).getState();
            logger.info("AMI state {} => {}", imageId, state);
            if ("available".equals(state)) {
                break;
            } else if ("failed".equals(state)) {
                throw new IllegalStateException("AMI failed to create, please check AWS console for more details");
            }
            Threads.sleepRoughly(Duration.ofSeconds(30));
        }
    }

    private String createAMI(Context context, String instanceId) throws Exception {
        AWS.getEc2().stopInstances(Lists.newArrayList(instanceId));

        logger.info("create AMI, instanceId={}, imageName={}", instanceId, resource.name());
        CreateImageResult result = AWS.getEc2().ec2.createImage(new CreateImageRequest(instanceId, resource.name()));
        String imageId = result.getImageId();

        AWS.getEc2().createTags(new CreateTagsRequest()
            .withResources(imageId)
            .withTags(tagHelper.env(),
                tagHelper.resourceId(resource.id),
                tagHelper.version(resource.nextVersion()),
                tagHelper.name(resource.id + ":" + resource.nextVersion())));

        String key = "ami/" + resource.id;
        context.output(key, String.format("imageId=%s", imageId));
        logger.info("result imageId => {}", imageId);
        return imageId;
    }

    private com.amazonaws.services.ec2.model.Instance createInstance(KeyPair keyPair, String sgId) throws Exception {
        RunInstancesRequest request = new RunInstancesRequest()
            .withKeyName(keyPair.remoteKeyPair.getKeyName())
            .withInstanceType(InstanceType.M4Large)
            .withImageId(resource.baseAMI.imageId())
            .withMinCount(1)
            .withMaxCount(1)
            .withSecurityGroupIds(sgId);

        if (bakeSubnet != null) request.withSubnetId(bakeSubnet.getSubnetId());

        return AWS.getEc2().runInstances(request,
            tagHelper.name(resourceId),
            tagHelper.env(),
            tagHelper.resourceId(resourceId),
            tagHelper.type("ami"),
            tagHelper.amiImageId(resource.id())).get(0);
    }

    private KeyPair createKeyPair(Environment env) throws IOException {
        KeyPair keyPair = new KeyPair(resourceId, env.name + ":" + resourceId);
        if (!AWS.getEc2().keyPairExists(keyPair.name)) {   // for resume previous failed baking, key pair may exist
            keyPairHelper.createKeyPair(keyPair);
        }
        return keyPair;
    }

    private String createSG(Environment env) throws Exception {
        String sgName = env.name + ":" + resourceId;
        CreateSecurityGroupRequest request = new CreateSecurityGroupRequest(sgName, sgName);
        if (bakeSubnet != null) request.setVpcId(bakeSubnet.getVpcId());

        String sgId = AWS.getEc2().createSecurityGroup(request).getGroupId();
        AWS.getEc2().createSGIngressRules(sgId, Lists.newArrayList(new IpPermission()
            .withIpv4Ranges(new IpRange().withCidrIp("0.0.0.0/0"))
            .withFromPort(22)
            .withToPort(22)
            .withIpProtocol("tcp")));

        AWS.getEc2().createTags(new CreateTagsRequest()
            .withResources(sgId)
            .withTags(tagHelper.name(resourceId), tagHelper.env(), tagHelper.resourceId(resourceId)));

        return sgId;
    }
}
