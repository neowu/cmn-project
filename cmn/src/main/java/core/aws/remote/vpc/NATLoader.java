package core.aws.remote.vpc;

import core.aws.client.AWS;
import core.aws.remote.EnvTag;
import core.aws.remote.Loader;
import core.aws.resource.Resources;
import core.aws.resource.ec2.Instance;
import core.aws.resource.ec2.InstanceState;
import core.aws.resource.vpc.NAT;
import core.aws.util.Lists;

import java.util.List;

/**
 * @author neo
 */
public class NATLoader extends Loader {
    public NATLoader(Resources resources, List<EnvTag> tags) {
        super(resources, tags);
    }

    @Override
    public void load() {
        all(Instance.class)
            .filter(tag -> "nat".equals(tag.type()))
            .forEach(tag -> {
                com.amazonaws.services.ec2.model.Instance remoteInstance = AWS.ec2.describeInstances(Lists.newArrayList(tag.remoteResourceId)).get(0);
                if (!InstanceState.TERMINATED.equalsTo(remoteInstance.getState())) {
                    loadNAT(tag.resourceId(), remoteInstance);
                }
            });
    }

    private void loadNAT(String resourceId, com.amazonaws.services.ec2.model.Instance remoteInstance) {
        NAT nat = resources.find(NAT.class, resourceId)
            .orElseGet(() -> resources.add(new NAT(resourceId)));
        nat.remoteInstance = remoteInstance;
        nat.foundInRemote();
    }
}
