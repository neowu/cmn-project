package core.aws.local.ec2;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * @author neo
 */
public class EBSBuilderTest {
    EBSBuilder builder;

    @Before
    public void createEBSBuilder() {
        builder = new EBSBuilder();
    }

    @Test
    public void parseSize() {
        assertThat(builder.parseSize("30G"), equalTo(30));
    }
}
