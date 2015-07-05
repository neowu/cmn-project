package core.aws.local.as;

import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import core.aws.resource.as.AutoScalingPolicy;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author neo
 */
public class ASGroupLoaderTest {
    ASGroupLoader loader;

    @Before
    public void createASGroupLoader() {
        loader = new ASGroupLoader();
    }

    @Test
    public void createPolicy() {
        AutoScalingPolicy policy = loader.createPolicy("as", "scale-out", ">=80%", "3min", "15%");

        assertEquals("as-scale-out", policy.id);
        assertEquals(80d, policy.cpuUtilizationPercentage, 0.0001);
        assertEquals(ComparisonOperator.GreaterThanOrEqualToThreshold, policy.comparisonOperator);
        assertEquals(3, policy.lastMinutes);
        assertEquals(15, policy.adjustmentPercentage);
    }
}
