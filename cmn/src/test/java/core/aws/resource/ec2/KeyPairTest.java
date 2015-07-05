package core.aws.resource.ec2;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author neo
 */
public class KeyPairTest {
    @Test
    public void normalizeName() {
        assertEquals("env-test", KeyPair.normalizeName("env:test"));
    }
}
