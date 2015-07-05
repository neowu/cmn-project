package core.aws.remote.vpc;

import core.aws.client.AWS;
import core.aws.remote.EnvTag;
import core.aws.remote.Loader;
import core.aws.resource.Resources;
import core.aws.resource.vpc.VPC;

import java.util.List;

/**
 * @author neo
 */
public class VPCLoader extends Loader {
    public VPCLoader(Resources resources, List<EnvTag> tags) {
        super(resources, tags);
    }

    @Override
    public void load() {
        all(VPC.class).forEach(tag -> {
            VPC vpc = resources.get(VPC.class, tag.resourceId());
            vpc.foundInRemote();
            vpc.remoteVPC = AWS.vpc.describeVPC(tag.remoteResourceId);
        });
    }
}
