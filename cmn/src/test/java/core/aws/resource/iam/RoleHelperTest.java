package core.aws.resource.iam;

import com.amazonaws.auth.policy.Policy;
import com.amazonaws.services.identitymanagement.model.Role;
import core.aws.util.ClasspathResources;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        Role remoteRole = new Role();
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
}