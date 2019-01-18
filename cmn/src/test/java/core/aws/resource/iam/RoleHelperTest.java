package core.aws.resource.iam;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.identitymanagement.model.Role;
import core.aws.util.ClasspathResources;
import core.aws.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author mort
 */
class RoleHelperTest {
    RoleHelper roleHelper;

    @BeforeEach
    void createRoleHelper() {
        this.roleHelper = new RoleHelper();
    }

    @Test
    void essentialChangedWithDifferentPath() {
        com.amazonaws.services.identitymanagement.model.Role remoteRole = new Role();
        remoteRole.setPath("/test");
        boolean changed = roleHelper.essentialChanged("/", null, remoteRole);
        assertTrue(changed);
    }

    @Test
    void policyChangedWithDifferentVersions() {
        Policy policy1 = Policy.fromJson(ClasspathResources.text("iam-test/assume-role-policy1.json"));
        Policy policy2 = Policy.fromJson(ClasspathResources.text("iam-test/assume-role-policy2.json"));
        boolean changed = roleHelper.policyChanged(policy1, policy2);
        assertTrue(changed);
    }

    @Test
    void attachedPolicyChanged() {
        // size not equals
        List<String> managedPolicyARNs = Lists.newArrayList("1", "2");
        List<String> remoteManagedPolicyARNS = Lists.newArrayList("2");
        boolean changed = roleHelper.attachedPolicyChanged(managedPolicyARNs, remoteManagedPolicyARNS);
        assertTrue(changed);

        // elements not equal
        remoteManagedPolicyARNS.add("3");
        changed = roleHelper.attachedPolicyChanged(managedPolicyARNs, remoteManagedPolicyARNS);
        assertTrue(changed);

        managedPolicyARNs.add("3");
        remoteManagedPolicyARNS.add("1");
        changed = roleHelper.attachedPolicyChanged(managedPolicyARNs, remoteManagedPolicyARNS);
        assertFalse(changed);
    }
}