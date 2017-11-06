package core.aws.local.ec2;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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
        assertThat(builder.parseSize("30G"), equalTo(30));
    }
}
