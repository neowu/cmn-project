package core.aws.local.as;

import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import core.aws.resource.as.AutoScalingPolicy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author neo
 */
class ASGroupLoaderTest {
    ASGroupLoader loader;

    @BeforeEach
    void createASGroupLoader() {
        loader = new ASGroupLoader();
    }

    @Test
    void createPolicy() {
        AutoScalingPolicy policy = loader.createPolicy("as", "scale-out", ">=80%", "3min", "15%");

        assertEquals("as-scale-out", policy.id);
        assertEquals(80d, policy.cpuUtilizationPercentage, 0.0001);
        assertEquals(ComparisonOperator.GreaterThanOrEqualToThreshold, policy.comparisonOperator);
        assertEquals(3, policy.lastMinutes);
        assertEquals(15, policy.adjustmentPercentage);
    }
}
