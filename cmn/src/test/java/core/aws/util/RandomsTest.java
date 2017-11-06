package core.aws.util;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author neo
 */
class RandomsTest {
    @Test
    void randomAlphaNumeric() {
        assertThat(Randoms.alphaNumeric(3).length(), equalTo(3));
        assertThat(Randoms.alphaNumeric(5).length(), equalTo(5));
        assertThat(Randoms.alphaNumeric(10).length(), equalTo(10));
    }
}
