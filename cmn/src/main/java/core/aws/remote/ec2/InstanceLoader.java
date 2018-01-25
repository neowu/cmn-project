package core.aws.remote.ec2;

import core.aws.client.AWS;
import core.aws.remote.EnvTag;
import core.aws.remote.Loader;
import core.aws.resource.Resources;
import core.aws.resource.ec2.Instance;
import core.aws.resource.ec2.InstanceState;
import core.aws.resource.ec2.KeyPair;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.resource.image.Image;
import core.aws.util.StreamHelper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * @author neo
 */
public class InstanceLoader extends Loader {
    public InstanceLoader(Resources resources, List<EnvTag> tags) {
        super(resources, tags);
    }

    @Override
    public void load() {
        Map<String, EnvTag> instanceIdLocalResourceIdMappings = new HashMap<>();

        all(Instance.class)
            .forEach(tag -> instanceIdLocalResourceIdMappings.put(tag.remoteResourceId, tag));

        if (!instanceIdLocalResourceIdMappings.isEmpty())
            loadInstances(instanceIdLocalResourceIdMappings);
    }

    private void loadInstances(Map<String, EnvTag> instanceIdLocalResourceIdMappings) {
        List<com.amazonaws.services.ec2.model.Instance> remoteInstances = AWS.getEc2().describeInstances(instanceIdLocalResourceIdMappings.keySet());
        // link sg and key pair for remote only instances
        remoteInstances.stream()
            .filter(remoteInstance -> !InstanceState.TERMINATED.equalsTo(remoteInstance.getState())
                && !InstanceState.SHUTTING_DOWN.equalsTo(remoteInstance.getState()))
            .forEach(remoteInstance -> {
                String instanceId = remoteInstance.getInstanceId();
                EnvTag tag = instanceIdLocalResourceIdMappings.get(instanceId);
                String resourceId = tag.resourceId();

                Instance instance = resources.find(Instance.class, resourceId).orElseGet(() -> {
                    Instance remoteOnlyInstance = new Instance(resourceId);

                    // link sg and key pair for remote only instances
                    String remoteSGId = remoteInstance.getSecurityGroups().get(0).getGroupId();
                    linkSecurityGroup(remoteOnlyInstance, remoteSGId);
                    String remoteKeyName = remoteInstance.getKeyName();
                    linkKeyPair(remoteOnlyInstance, remoteKeyName);

                    resources.add(remoteOnlyInstance);
                    return remoteOnlyInstance;
                });

                if ("ami".equals(tag.type())) {
                    resources.find(Image.class, tag.amiImageId()).ifPresent(image -> image.unfinishedBakeInstances.add(instance));
                }

                instance.foundInRemote();
                instance.remoteInstances.add(remoteInstance);
            });
    }

    private void linkKeyPair(Instance instance, String remoteKeyName) {
        Optional<KeyPair> keyPair = resources.stream().flatMap(StreamHelper.instanceOf(KeyPair.class))
            .filter(key -> key.remoteKeyPair != null && remoteKeyName.equals(key.remoteKeyPair.getKeyName()))
            .reduce(StreamHelper.onlyOne());
        if (keyPair.isPresent()) instance.keyPair = keyPair.get();
    }

    private void linkSecurityGroup(Instance instance, String remoteSGId) {
        Optional<SecurityGroup> securityGroup = resources.stream().flatMap(StreamHelper.instanceOf(SecurityGroup.class))
            .filter(sg -> sg.remoteSecurityGroup != null && remoteSGId.equals(sg.remoteSecurityGroup.getGroupId()))
            .reduce(StreamHelper.onlyOne());
        if (securityGroup.isPresent()) instance.securityGroup = securityGroup.get();
    }
}
