package core.aws.local.vpc;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.vpc.InternetGateway;
import core.aws.resource.vpc.NATGateway;
import core.aws.resource.vpc.RouteTable;
import core.aws.resource.vpc.Subnet;
import core.aws.resource.vpc.SubnetType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author neo
 */
public class SubnetLoader implements LocalResourceLoader {
    @Override
    @SuppressWarnings("unchecked")
    public void load(ResourceNode node, final Resources resources, DependencyResolvers resolvers, Environment env) {
        List<String> cidrs = new ArrayList<>();
        final SubnetType type = SubnetType.valueOf(node.getString("type").orElse("PUBLIC").toUpperCase());

        Object cidr = node.field("cidr");
        if (cidr instanceof String) cidrs.add((String) cidr);
        else if (cidr instanceof List) cidrs.addAll((java.util.Collection<? extends String>) cidr);

        final Subnet subnet = new Subnet(node.id);
        subnet.cidrs.addAll(cidrs);
        subnet.type = type;
        subnet.vpc = resources.vpc;

        if (type == SubnetType.PUBLIC) {
            addInternetGateway(node, resources, resolvers);
            addPublicRouteTable(node, resources, resolvers);

            node.getString("nat").ifPresent(ip -> {
                final NATGateway nat = resources.add(new NATGateway());
                nat.subnet = subnet;
                nat.ip = ip;
            });

        } else {
            addPrivateRouteTable(node, resources, resolvers);
        }

        resources.add(subnet);

        resolvers.add(node, () -> {
            String routeTableId = type == SubnetType.PUBLIC ? RouteTable.PUBLIC_ROUTE_TABLE_RESOURCE_ID : RouteTable.PRIVATE_ROUTE_TABLE_RESOURCE_ID;
            subnet.routeTable = resources.get(RouteTable.class, routeTableId);
        });
    }


    private void addPrivateRouteTable(ResourceNode node, final Resources resources, DependencyResolvers resolvers) {
        if (!resources.find(RouteTable.class, RouteTable.PRIVATE_ROUTE_TABLE_RESOURCE_ID).isPresent()) {
            final RouteTable routeTable = resources.add(new RouteTable(RouteTable.PRIVATE_ROUTE_TABLE_RESOURCE_ID));
            resolvers.add(node, () -> {
                routeTable.vpc = resources.vpc;
                routeTable.nat = resources.onlyOne(NATGateway.class).get();
            });
        }
    }

    private void addPublicRouteTable(ResourceNode node, final Resources resources, DependencyResolvers resolvers) {
        if (!resources.find(RouteTable.class, RouteTable.PUBLIC_ROUTE_TABLE_RESOURCE_ID).isPresent()) {
            final RouteTable routeTable = resources.add(new RouteTable(RouteTable.PUBLIC_ROUTE_TABLE_RESOURCE_ID));
            resolvers.add(node, () -> {
                routeTable.vpc = resources.vpc;
                routeTable.internetGateway = resources.onlyOne(InternetGateway.class).get();
            });
        }
    }

    private void addInternetGateway(ResourceNode node, Resources resources, DependencyResolvers resolvers) {
        if (!resources.onlyOne(InternetGateway.class).isPresent()) {
            final InternetGateway internetGateway = resources.add(new InternetGateway());
            resolvers.add(node, () -> internetGateway.vpc = resources.vpc);
        }
    }
}
