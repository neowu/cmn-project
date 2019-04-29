package core.aws.resource.ec2;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Condition;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.services.identitymanagement.model.Role;
import core.aws.client.AWS;
import core.aws.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
class InstanceProfileHelper {
    private final Logger logger = LoggerFactory.getLogger(InstanceProfileHelper.class);

    void validatePolicyDocument(String policyJSON) {
        Policy policy = Policy.fromJson(policyJSON);
        Asserts.isFalse(policy.getStatements().isEmpty(), "statement is required");
        for (Statement statement : policy.getStatements()) {
            Asserts.isFalse(statement.getActions().isEmpty(), "action is required");
        }
    }

    boolean policyChanged(String localPolicyJSON, com.amazonaws.services.identitymanagement.model.InstanceProfile remoteInstanceProfile) {
        String instanceProfileName = remoteInstanceProfile.getInstanceProfileName();
        List<Role> roles = remoteInstanceProfile.getRoles();
        Asserts.isFalse(roles.isEmpty(), "instance profile does not not have role, please check whether the role failed to add to instance profile, instanceProfileName={}",
            instanceProfileName);
        Asserts.equals(roles.size(), 1, "instance profile should only have one role, check whether it's modified not by cmn, instanceProfileName={}, roles={}",
            instanceProfileName, roles);

        Role role = roles.get(0);
        Optional<Policy> remotePolicy = AWS.getIam().findRolePolicy(role.getRoleName(), role.getRoleName());
        if (!remotePolicy.isPresent()) {
            logger.warn("role policy doesn't exist, it could be due to failure of last sync, it will try to create this time, instanceProfileName={}", instanceProfileName);
            return true;
        }

        Policy localPolicy = Policy.fromJson(localPolicyJSON);

        return policyChanged(localPolicy, remotePolicy.get());
    }

    boolean policyChanged(Policy policy1, Policy policy2) {
        Collection<Statement> statements1 = policy1.getStatements();
        Collection<Statement> statements2 = policy2.getStatements();
        if (statements1.size() != statements2.size()) return true;

        for (Statement statement1 : statements1) {
            if (!containStatement(statements2, statement1)) return true;
        }

        return false;
    }

    private boolean containStatement(Collection<Statement> statements, Statement statement) {
        return statements.stream().anyMatch(statement1 -> statementEquals(statement1, statement));
    }

    private Boolean statementEquals(Statement statement1, Statement statement2) {
        List<Action> actions1 = statement1.getActions();
        List<Action> actions2 = statement2.getActions();
        boolean actionMatches = actions1.size() == actions2.size()
            && actions1.stream().allMatch(action1 -> actions2.stream().anyMatch(action2 -> action1.getActionName().equals(action2.getActionName())));
        if (!actionMatches) return Boolean.FALSE;

        boolean effectMatches = statement1.getEffect() == statement2.getEffect();
        if (!effectMatches) return Boolean.FALSE;

        List<Resource> resources1 = statement1.getResources();
        List<Resource> resources2 = statement2.getResources();
        boolean resourceMatches = resources1.size() == resources2.size()
            && resources1.stream().allMatch(resource1 -> resources2.stream().anyMatch(resource2 -> resource1.getId().equals(resource2.getId())));
        if (!resourceMatches) return Boolean.FALSE;

        List<Condition> conditions1 = statement1.getConditions();
        List<Condition> conditions2 = statement2.getConditions();
        boolean conditionMatches = conditions1.size() == conditions2.size()
            && conditions1.stream().allMatch(condition1 -> conditions2.stream().anyMatch(condition2 -> conditionEquals(condition1, condition2)));
        if (!conditionMatches) return Boolean.FALSE;

        List<Principal> principals1 = statement1.getPrincipals();
        List<Principal> principals2 = statement2.getPrincipals();
        return principals1.size() == principals2.size()
            && principals1.stream().allMatch(principle1 -> principals2.stream().anyMatch(principal2 -> principleEquals(principle1, principal2)));
    }

    private boolean principleEquals(Principal principle1, Principal principal2) {
        return principle1.getId().equals(principal2.getId())
            && principle1.getProvider().equals(principal2.getProvider());
    }

    private boolean conditionEquals(Condition condition1, Condition condition2) {
        if (!condition1.getConditionKey().equals(condition2.getConditionKey())) return false;
        if (!condition1.getType().equals(condition2.getType())) return false;

        List<String> values2 = condition2.getValues();
        if (condition1.getValues().size() != values2.size()) return false;
        for (String value : condition1.getValues()) {
            if (!values2.contains(value)) return false;
        }

        return true;
    }
}
