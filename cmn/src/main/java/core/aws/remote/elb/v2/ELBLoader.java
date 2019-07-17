package core.aws.remote.elb.v2;

import com.amazonaws.services.elasticloadbalancingv2.model.AvailabilityZone;
import com.amazonaws.services.elasticloadbalancingv2.model.DescribeTagsRequest;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.elb.v2.ELB;
import core.aws.resource.vpc.Subnet;
import core.aws.util.Lists;

import java.util.List;

public class ELBLoader {
    private final Resources resources;
    private final Environment env;

    public ELBLoader(Resources resources, Environment env) {
        this.resources = resources;
        this.env = env;
    }

    public void load() {
        List<LoadBalancer> remoteELBs = AWS.getElbV2().listELBs();
        for (LoadBalancer remoteELB : remoteELBs) {
            String elbName = remoteELB.getLoadBalancerName();
            String prefix = env.name + "-";
            if (elbName.startsWith(prefix) && "2".equals(getVersion(remoteELB.getLoadBalancerArn()))) {
                String resourceId = elbName.substring(prefix.length());
                ELB elb = resources.find(ELB.class, resourceId).orElseGet(() -> resources.add(new ELB(resourceId)));
                elb.name = elbName;
                elb.remoteELB = remoteELB;
                List<Subnet> subnets = Lists.newArrayList();
                remoteELB.getAvailabilityZones().stream().map(AvailabilityZone::getSubnetId).forEach(subnetId -> resources.find(Subnet.class, subnetId).ifPresent(subnets::add));
                elb.subnets = subnets;
                elb.foundInRemote();
            }
        }
    }

    private String getVersion(String elbARN) {
        List<Tag> tags = AWS.getElbV2().elb.describeTags(new DescribeTagsRequest()
            .withResourceArns(elbARN)).getTagDescriptions().get(0).getTags();
        return tags.stream().filter(tag -> "cloud-manager:elb-version".equals(tag.getKey())).map(Tag::getValue).findFirst().orElse("1");
    }
}
