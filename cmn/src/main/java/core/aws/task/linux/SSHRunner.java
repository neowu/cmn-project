package core.aws.task.linux;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.env.Environment;
import core.aws.env.Param;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.resource.as.AutoScalingGroup;
import core.aws.resource.ec2.Instance;
import core.aws.resource.ec2.KeyPair;
import core.aws.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author neo
 */
public class SSHRunner {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Resources resources;
    private final Environment env;
    private final Context context;

    public SSHRunner(Resources resources, Environment env, Context context) {
        this.resources = resources;
        this.env = env;
        this.context = context;
    }

    public void run() throws IOException, InterruptedException {
        String resourceId = context.requiredParam(Param.RESOURCE_ID);
        Optional<Instance> instance = resources.find(Instance.class, resourceId);
        if (instance.isPresent() && instance.get().status == ResourceStatus.LOCAL_REMOTE) {
            logger.info("ssh to instance/{}", instance.get().id);
            ssh(instance.get().remoteInstances);
            return;
        }

        Optional<AutoScalingGroup> asGroup = resources.find(AutoScalingGroup.class, resourceId);
        if (asGroup.isPresent() && asGroup.get().status == ResourceStatus.LOCAL_REMOTE) {
            logger.info("ssh to asg/{}", asGroup.get().id);
            List<String> instanceIds = asGroup.get().remoteASGroup.getInstances().stream().map(com.amazonaws.services.autoscaling.model.Instance::getInstanceId).collect(Collectors.toList());
            List<com.amazonaws.services.ec2.model.Instance> remoteInstances = AWS.ec2.describeInstances(instanceIds);
            ssh(remoteInstances);
            return;
        }

        throw new IllegalStateException("can not find any running instance or asGroup, id=" + resourceId);
    }

    private void ssh(List<com.amazonaws.services.ec2.model.Instance> remoteInstances) throws IOException, InterruptedException {
        int remoteInstanceCount = remoteInstances.size();

        for (int i = 0; i < remoteInstanceCount; i++) {
            com.amazonaws.services.ec2.model.Instance remoteInstance = remoteInstances.get(i);
            logger.info("index={}, instanceId={}, state={}, publicDNS={}, privateDNS={}",
                i,
                remoteInstance.getInstanceId(),
                remoteInstance.getState().getName(),
                remoteInstance.getPublicDnsName(),
                remoteInstance.getPrivateDnsName());
        }

        Asserts.isTrue(remoteInstanceCount > 0, "there is no remoteInstance to ssh");

        String index = context.param(Param.INSTANCE_INDEX);
        Asserts.isTrue(remoteInstanceCount == 1 || index != null, "more than one remoteInstance, use --{} to specify index", Param.INSTANCE_INDEX.key);

        com.amazonaws.services.ec2.model.Instance remoteInstance = remoteInstanceCount == 1 ? remoteInstances.get(0) : remoteInstances.get(Integer.parseInt(index));

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
