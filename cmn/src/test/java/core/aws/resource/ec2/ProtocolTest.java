package core.aws.resource.ec2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author neo
 */
class ProtocolTest {
    @Test
    void parsePortRange() {
        Protocol protocol = Protocol.parse("10000-12345");
        assertEquals(Protocol.TCP, protocol.ipProtocol);
        assertEquals(10000, protocol.fromPort);
        assertEquals(12345, protocol.toPort);
    }

    @Test
    void parseSinglePort() {
        Protocol protocol = Protocol.parse("9300");
        assertEquals(Protocol.TCP, protocol.ipProtocol);
        assertEquals(9300, protocol.fromPort);
        assertEquals(9300, protocol.toPort);
    }

    @Test
    void parsePredefinedProtocol() {
        Protocol protocol = Protocol.parse("http");
        assertEquals(Protocol.TCP, protocol.ipProtocol);
        assertEquals(80, protocol.fromPort);
        assertEquals(80, protocol.toPort);
    }
}
