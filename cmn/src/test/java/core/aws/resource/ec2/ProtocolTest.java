package core.aws.resource.ec2;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author neo
 */
public class ProtocolTest {
    @Test
    public void parsePortRange() {
        Protocol protocol = Protocol.parse("10000-12345");
        Assert.assertEquals(Protocol.TCP, protocol.ipProtocol);
        Assert.assertEquals(10000, protocol.fromPort);
        Assert.assertEquals(12345, protocol.toPort);
    }

    @Test
    public void parseSinglePort() {
        Protocol protocol = Protocol.parse("9300");
        Assert.assertEquals(Protocol.TCP, protocol.ipProtocol);
        Assert.assertEquals(9300, protocol.fromPort);
        Assert.assertEquals(9300, protocol.toPort);
    }

    @Test
    public void parsePredefinedProtocol() {
        Protocol protocol = Protocol.parse("http");
        Assert.assertEquals(Protocol.TCP, protocol.ipProtocol);
        Assert.assertEquals(80, protocol.fromPort);
        Assert.assertEquals(80, protocol.toPort);
    }
}