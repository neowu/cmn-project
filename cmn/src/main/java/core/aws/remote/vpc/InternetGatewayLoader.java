package core.aws.remote.vpc;

import core.aws.remote.EnvTag;
import core.aws.remote.Loader;
import core.aws.resource.Resources;
import core.aws.resource.vpc.InternetGateway;
import core.aws.util.StreamHelper;

import java.util.List;

/**
 * @author neo
 */
public class InternetGatewayLoader extends Loader {
    public InternetGatewayLoader(Resources resources, List<EnvTag> tags) {
        super(resources, tags);
    }

    @Override
    public void load() {
        // should only have no more than one internet gateway
        all(InternetGateway.class).reduce(StreamHelper.onlyOne())
            .ifPresent(tag -> {
                // should have local one if there is remote one
                InternetGateway internetGateway = resources.onlyOne(InternetGateway.class).get();
                internetGateway.foundInRemote();
                internetGateway.remoteInternetGatewayId = tag.remoteResourceId;
            });
    }
}
