package core.aws.resource.ec2;

import com.amazonaws.auth.policy.Policy;
import core.aws.util.ClasspathResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author neo
 */
class InstanceProfileHelperTest {
    InstanceProfileHelper instanceProfileHelper;

    @BeforeEach
    void createInstanceProfileHelper() {
        instanceProfileHelper = new InstanceProfileHelper();
    }

    @Test
    void policyChangedWithSamePolicy() {
        Policy policy1 = Policy.fromJson(ClasspathResources.text("iam-test/policy1.json"));
        Policy policy2 = Policy.fromJson(ClasspathResources.text("iam-test/policy1.json"));
        boolean changed = instanceProfileHelper.policyChanged(policy1, policy2);

        assertFalse(changed);
    }

    @Test
    void policyChangedWithDifferentPolicies() {
        Policy policy1 = Policy.fromJson(ClasspathResources.text("iam-test/policy1.json"));
        Policy policy2 = Policy.fromJson(ClasspathResources.text("iam-test/policy2.json"));
        boolean changed = instanceProfileHelper.policyChanged(policy1, policy2);

        assertTrue(changed);
    }
}
