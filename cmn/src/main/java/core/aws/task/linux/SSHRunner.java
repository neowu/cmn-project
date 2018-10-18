package core.aws.task.linux;

import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagDescription;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.env.Param;
import core.aws.resource.ec2.KeyPair;
import core.aws.task.ec2.EC2TagHelper;
import core.aws.util.Asserts;
import core.aws.util.Lists;
import core.aws.util.Randoms;
import core.aws.util.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

/**
 * @author neo
 */
public class SSHRunner {
    private final Logger logger = LoggerFactory.getLogger(SSHRunner.class);

    private final Environment env;
    private final String resourceId;
    private final Integer instanceIndex;
    private final String tunnelResourceId;

    public SSHRunner(Environment env, String resourceId, Integer instanceIndex, String tunnelResourceId) {
        this.env = env;
        this.resourceId = resourceId;
        this.instanceIndex = instanceIndex;
        this.tunnelResourceId = tunnelResourceId;
    }

    public void run() throws IOException, InterruptedException {
        List<Instance> instances = runningInstances(resourceId);
        Instance instance = locateInstanceToSSH(instances);
        Integer tunnelPort = null;
        if (tunnelResourceId != null) {
            tunnelPort = startTunnelSSH(instance);
        }
        ssh(instance, tunnelPort);
    }

    private Integer startTunnelSSH(Instance instance) throws InterruptedException {
        Instance tunnelInstance = runningInstances(tunnelResourceId).get(0);
        Integer localPort = (int) Randoms.number(3000, 10000);
        CountDownLatch latch = new CountDownLatch(1);
        Thread tunnelThread = new Thread(() -> {
            Process process = null;
            try {
                Path keyPath = KeyPair.keyFile(tunnelInstance.getKeyName(), env);
                String userAndHost = "ubuntu@" + hostName(tunnelInstance);
                String portBinding = Strings.format("{}:{}:22", localPort, instance.getPrivateIpAddress());
                List<String> command = tunnelCommand(keyPath, userAndHost, portBinding);
                logger.info("tunnel command => {}", String.join(" ", command));
                process = new ProcessBuilder().command(command).start();
                process.getInputStream().read();    // wait until there is output
                latch.countDown();
                process.waitFor();
            } catch (InterruptedException | IOException e) {
                throw new IllegalStateException(e);
            } finally {
                if (process != null) process.destroy();
            }
        });
        tunnelThread.setDaemon(true);
        tunnelThread.start();
        latch.await();
        return localPort;
    }

    private List<Instance> runningInstances(String resourceId) {
        Tag tag = new EC2TagHelper(env).resourceId(resourceId);
        DescribeTagsRequest request = new DescribeTagsRequest()
            .withFilters(new Filter("key").withValues(tag.getKey()),
                new Filter("value").withValues(tag.getValue()),
                new Filter("resource-type").withValues("instance"));
        List<TagDescription> remoteTags = AWS.getEc2().describeTags(request);
        List<String> instanceIds = remoteTags.stream().map(TagDescription::getResourceId).collect(Collectors.toList());

        if (instanceIds.isEmpty()) {
            com.amazonaws.services.autoscaling.model.AutoScalingGroup asGroup = AWS.getAs().describeASGroup(env.name + "-" + this.resourceId);
            if (asGroup == null) throw new Error("can not find any running instance or asGroup, id=" + this.resourceId);

            instanceIds = asGroup.getInstances().stream()
                .map(com.amazonaws.services.autoscaling.model.Instance::getInstanceId)
                .collect(Collectors.toList());
        }

        logger.info("find instanceId, {} => {}", resourceId, instanceIds);

        List<Instance> instances = AWS.getEc2().describeInstances(instanceIds)
            .stream().filter(instance -> "running".equals(instance.getState().getName())).collect(Collectors.toList());
        if (instances.isEmpty()) throw new Error("can not find any running instance, id=" + resourceId);

        return instances;
    }

    private Instance locateInstanceToSSH(List<Instance> instances) {
        for (int i = 0; i < instances.size(); i++) {
            Instance remoteInstance = instances.get(i);
            logger.info("index={}, instanceId={}, state={}, publicDNS={}, privateDNS={}",
                i,
                remoteInstance.getInstanceId(),
                remoteInstance.getState().getName(),
                remoteInstance.getPublicDnsName(),
                remoteInstance.getPrivateDnsName());
        }
        Asserts.isTrue(instanceIndex != null || instances.size() == 1, "more than one remoteInstance, use --{} to specify index", Param.INSTANCE_INDEX.key);
        return instances.size() == 1 ? instances.get(0) : instances.get(instanceIndex);
    }

    private void ssh(Instance instance, Integer tunnelPort) throws IOException, InterruptedException {
        Path keyPath = KeyPair.keyFile(instance.getKeyName(), env);
        String userAndHost;
        if (tunnelPort != null) userAndHost = "ubuntu@localhost";
        else userAndHost = "ubuntu@" + hostName(instance);
        List<String> command = command(keyPath, userAndHost, tunnelPort);
        logger.info("command => {}", String.join(" ", command));
        Process process = new ProcessBuilder().inheritIO().command(command).start();
        process.waitFor();
        logger.info("session disconnected");
    }

    private List<String> command(Path keyPath, String userAndHost, Integer tunnelPort) {
        List<String> command = Lists.newArrayList();
        if (System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("win")) {
            command.add("cmd");
            command.add("/C");
            command.add("start");
        }
        // send server stay live signal every 30 seconds, and accept host
        List<String> params = Lists.newArrayList("ssh", "-o", "ServerAliveInterval=30", "-o", "StrictHostKeyChecking=no", "-i", keyPath.toString());
        if (tunnelPort != null) {
            params.add("-p");
            params.add(String.valueOf(tunnelPort));
        }
        params.add(userAndHost);
        command.addAll(params);
        return command;
    }

    private List<String> tunnelCommand(Path keyPath, String userAndHost, String portBinding) {
        List<String> command = Lists.newArrayList();
        if (System.getProperty("os.name").toLowerCase(Locale.getDefault()).contains("win")) {
            command.add("cmd");
            command.add("/C");
            command.add("start");
        }
        command.addAll(Lists.newArrayList("ssh", "-o", "ServerAliveInterval=30", "-o", "StrictHostKeyChecking=no", "-i", keyPath.toString(), "-L", portBinding, userAndHost));
        return command;
    }

    private String hostName(Instance remoteInstance) {
        String publicDNS = remoteInstance.getPublicDnsName();
        return publicDNS != null ? publicDNS : remoteInstance.getPrivateDnsName();
    }
}
