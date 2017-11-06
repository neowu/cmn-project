package core.aws.resource.vpc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author neo
 */
class SubnetTest {
    @Test
    void firstRemoteSubnet() {
        Subnet subnet = new Subnet("public");
        subnet.remoteSubnets.add(new com.amazonaws.services.ec2.model.Subnet().withSubnetId("1").withAvailabilityZone("us-east-1b"));
        subnet.remoteSubnets.add(new com.amazonaws.services.ec2.model.Subnet().withSubnetId("2").withAvailabilityZone("us-east-1a"));

        assertEquals("us-east-1a", subnet.firstRemoteSubnet().getAvailabilityZone());
    }
}
