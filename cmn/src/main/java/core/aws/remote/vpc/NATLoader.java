package core.aws.remote.vpc;

import com.amazonaws.services.ec2.model.DescribeNatGatewaysRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.NatGateway;
import core.aws.client.AWS;
import core.aws.remote.EnvTag;
import core.aws.remote.Loader;
import core.aws.resource.Resources;
import core.aws.resource.vpc.NATGateway;
import core.aws.resource.vpc.VPC;
import core.aws.util.Strings;

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
        VPC vpc = resources.onlyOne(VPC.class).get();

        if (vpc.remoteVPC != null) {
            List<NatGateway> gateways = AWS.vpc.ec2.describeNatGateways(new DescribeNatGatewaysRequest()
                .withFilter(new Filter("state").withValues("available"),
                    new Filter("vpc-id").withValues(vpc.remoteVPC.getVpcId())))
                .getNatGateways();

            if (gateways.size() > 1) throw new Error(Strings.format("multiple nat gateway found, gateways={}", gateways));
            if (!gateways.isEmpty()) {
                NATGateway gateway = resources.onlyOne(NATGateway.class).orElseGet(() -> resources.add(new NATGateway()));
                gateway.remoteNATGateway = gateways.get(0);
                gateway.foundInRemote();
            }
        }
    }
}
