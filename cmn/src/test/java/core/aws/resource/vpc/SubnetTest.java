package core.aws.resource.vpc;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author neo
 */
public class SubnetTest {
    @Test
    public void firstRemoteSubnet() {
        Subnet subnet = new Subnet("public");
        subnet.remoteSubnets.add(new com.amazonaws.services.ec2.model.Subnet().withSubnetId("1").withAvailabilityZone("us-east-1b"));
        subnet.remoteSubnets.add(new com.amazonaws.services.ec2.model.Subnet().withSubnetId("2").withAvailabilityZone("us-east-1a"));

        Assert.assertEquals("us-east-1a", subnet.firstRemoteSubnet().getAvailabilityZone());
    }
}