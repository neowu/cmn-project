package core.aws.remote.vpc;

import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import core.aws.client.AWS;
import core.aws.remote.EnvTag;
import core.aws.remote.Loader;
import core.aws.resource.Resources;
import core.aws.resource.vpc.Subnet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author neo
 */
public class SubnetLoader extends Loader {
    public SubnetLoader(Resources resources, List<EnvTag> tags) {
        super(resources, tags);
    }

    @Override
    public void load() {
        Map<String, Subnet> remoteSubnets = new HashMap<>();

        all(Subnet.class).forEach(tag -> {
            Subnet subnet = resources.find(Subnet.class, tag.resourceId())
                .orElseGet(() -> resources.add(new Subnet(tag.resourceId())));
            subnet.foundInRemote();
            remoteSubnets.put(tag.remoteResourceId, subnet);
        });

        if (!remoteSubnets.isEmpty())
            loadRemoteSubnets(remoteSubnets);
    }

    private void loadRemoteSubnets(Map<String, Subnet> remoteSubnets) {
        DescribeSubnetsResult result = AWS.vpc.ec2.describeSubnets(new DescribeSubnetsRequest().withSubnetIds(remoteSubnets.keySet()));
        for (com.amazonaws.services.ec2.model.Subnet remoteSubnet : result.getSubnets()) {
            remoteSubnets.get(remoteSubnet.getSubnetId()).remoteSubnets.add(remoteSubnet);
        }
    }
}
