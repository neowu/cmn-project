package core.aws.remote.vpc;

import core.aws.client.AWS;
import core.aws.remote.EnvTag;
import core.aws.remote.Loader;
import core.aws.resource.Resources;
import core.aws.resource.vpc.RouteTable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author neo
 */
public class RouteTableLoader extends Loader {
    public RouteTableLoader(Resources resources, List<EnvTag> tags) {
        super(resources, tags);
    }

    @Override
    public void load() {
        Map<String, RouteTable> remoteRouteTables = new HashMap<>();

        all(RouteTable.class).forEach(tag -> {
            RouteTable routeTable = resources.find(RouteTable.class, tag.resourceId())
                .orElseGet(() -> resources.add(new RouteTable(tag.resourceId())));
            routeTable.foundInRemote();
            remoteRouteTables.put(tag.remoteResourceId, routeTable);
        });

        if (!remoteRouteTables.isEmpty())
            loadRemoteRouteTables(remoteRouteTables);
    }

    private void loadRemoteRouteTables(Map<String, RouteTable> remoteRouteTables) {
        for (com.amazonaws.services.ec2.model.RouteTable remoteRouteTable : AWS.vpc.describeRouteTables(remoteRouteTables.keySet())) {
            RouteTable routeTable = remoteRouteTables.get(remoteRouteTable.getRouteTableId());
            routeTable.remoteRouteTable = remoteRouteTable;
        }
    }
}
