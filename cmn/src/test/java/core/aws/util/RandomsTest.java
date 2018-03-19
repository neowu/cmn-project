package core.aws.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author neo
 */
class RandomsTest {
    @Test
    void randomAlphaNumeric() {
        assertThat(Randoms.alphaNumeric(3)).hasSize(3);
        assertThat(Randoms.alphaNumeric(5)).hasSize(5);
        assertThat(Randoms.alphaNumeric(10)).hasSize(10);
    }
}
