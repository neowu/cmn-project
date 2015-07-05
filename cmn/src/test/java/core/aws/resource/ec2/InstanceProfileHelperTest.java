package core.aws.resource.ec2;

import com.amazonaws.auth.policy.Policy;
import core.aws.util.ClasspathResources;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author neo
 */
public class InstanceProfileHelperTest {
    InstanceProfileHelper instanceProfileHelper;

    @Before
    public void createInstanceProfileHelper() {
        instanceProfileHelper = new InstanceProfileHelper();
    }

    @Test
    public void policyChangedWithSamePolicy() {
        Policy policy1 = Policy.fromJson(ClasspathResources.text("iam-test/policy1.json"));
        Policy policy2 = Policy.fromJson(ClasspathResources.text("iam-test/policy1.json"));
        boolean changed = instanceProfileHelper.policyChanged(policy1, policy2);

        Assert.assertFalse(changed);
    }

    @Test
    public void policyChangedWithDifferentPolicies() {
        Policy policy1 = Policy.fromJson(ClasspathResources.text("iam-test/policy1.json"));
        Policy policy2 = Policy.fromJson(ClasspathResources.text("iam-test/policy2.json"));
        boolean changed = instanceProfileHelper.policyChanged(policy1, policy2);

        Assert.assertTrue(changed);
    }
}