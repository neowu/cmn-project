package core.aws.local.ec2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author neo
 */
class EBSBuilderTest {
    EBSBuilder builder;

    @BeforeEach
    void createEBSBuilder() {
        builder = new EBSBuilder();
    }

    @Test
    void parseSize() {
        assertThat(builder.parseSize("30G")).isEqualTo(30);
    }
}
