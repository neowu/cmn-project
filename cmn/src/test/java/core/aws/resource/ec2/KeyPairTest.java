package core.aws.resource.ec2;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author neo
 */
class KeyPairTest {
    @Test
    void normalizeName() {
        assertEquals("env-test", KeyPair.normalizeName("env:test"));
    }
}
