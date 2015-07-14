package core.aws.task.linux;

import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagDescription;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.env.Param;
import core.aws.resource.ec2.KeyPair;
import core.aws.task.ec2.EC2TagHelper;
import core.aws.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author neo
 */
public class SSHRunner {
    private final Logger logger = LoggerFactory.getLogger(SSHRunner.class);

    private final Environment env;
    private final String resourceId;
    private final Integer instanceIndex;

    public SSHRunner(Environment env, String resourceId, Integer instanceIndex) {
        this.env = env;
        this.resourceId = resourceId;
        this.instanceIndex = instanceIndex;
    }

    public void run() throws IOException, InterruptedException {
        Tag tag = new EC2TagHelper(env).resourceId(resourceId);
        DescribeTagsRequest request = new DescribeTagsRequest()
            .withFilters(new Filter("key").withValues(tag.getKey()),
                new Filter("value").withValues(tag.getValue()),
                new Filter("resource-type").withValues("instance"));
        List<TagDescription> remoteTags = AWS.ec2.describeTags(request);
        List<String> instanceIds = remoteTags.stream().map(TagDescription::getResourceId).collect(Collectors.toList());

        if (!instanceIds.isEmpty()) {
            logger.info("ssh to instance/{}", resourceId);
        } else {
            com.amazonaws.services.autoscaling.model.AutoScalingGroup asGroup = AWS.as.describeASGroup(env.name + "-" + resourceId);
            if (asGroup == null) throw new Error("can not find any running instance or asGroup, id=" + resourceId);

            logger.info("ssh to asg/{}", resourceId);
            instanceIds = asGroup.getInstances().stream()
                .map(com.amazonaws.services.autoscaling.model.Instance::getInstanceId)
                .collect(Collectors.toList());
        }

        ssh(instanceIds);
    }

    private void ssh(List<String> instanceIds) throws IOException, InterruptedException {
        List<com.amazonaws.services.ec2.model.Instance> instances = AWS.ec2.describeInstances(instanceIds)
            .stream().filter(instance -> "running".equals(instance.getState().getName())).collect(Collectors.toList());

        if (instances.isEmpty()) throw new Error("can not find any running instance, id=" + resourceId);

        for (int i = 0; i < instances.size(); i++) {
            com.amazonaws.services.ec2.model.Instance remoteInstance = instances.get(i);
            logger.info("index={}, instanceId={}, state={}, publicDNS={}, privateDNS={}",
                i,
                remoteInstance.getInstanceId(),
                remoteInstance.getState().getName(),
                remoteInstance.getPublicDnsName(),
                remoteInstance.getPrivateDnsName());
        }
        Asserts.isTrue(instances.size() == 1 || instanceIndex != null, "more than one remoteInstance, use --{} to specify index", Param.INSTANCE_INDEX.key);
        com.amazonaws.services.ec2.model.Instance remoteInstance = instances.size() == 1 ? instances.get(0) : instances.get(instanceIndex);

        String state = remoteInstance.getState().getName();
        Asserts.equals(state, "running", "remoteInstance is not running, state={}", state);

        Path keyPath = KeyPair.keyFile(remoteInstance.getKeyName(), env);
        String userAndHost = "ubuntu@" + hostName(remoteInstance);
        List<String> command = command(keyPath, userAndHost);
        logger.info("command => {}", String.join(" ", command));
        Process process = new ProcessBuilder().inheritIO().command(command).start();
        process.waitFor();
        logger.info("session disconnected");
    }

    private List<String> command(Path keyPath, String userAndHost) {
        List<String> command = new ArrayList<>();
        if (System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("win")) {
            command.add("cmd");
            command.add("/C");
            command.add("start");
        }
        // send server stay live signal every 30 seconds, and accept host
        command.addAll(Arrays.asList("ssh", "-o", "ServerAliveInterval=30", "-o", "StrictHostKeyChecking=no", "-i", keyPath.toString(), userAndHost));
        return command;
    }

    private String hostName(com.amazonaws.services.ec2.model.Instance remoteInstance) {
        String publicDNS = remoteInstance.getPublicDnsName();
        return publicDNS != null ? publicDNS : remoteInstance.getPrivateDnsName();
    }
}
