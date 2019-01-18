package core.aws.resource.iam;

import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Statement;
import core.aws.util.Asserts;
import core.aws.util.Encodings;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @author mort
 */
public class RoleHelper {
    void validatePolicyDocument(String policyJSON) {
        Policy policy = Policy.fromJson(policyJSON);
        Asserts.isFalse(policy.getVersion().isEmpty(), "version is required");
        Asserts.isFalse(policy.getStatements().isEmpty(), "statement is required");
        for (Statement statement : policy.getStatements()) {
            Asserts.isFalse(statement.getPrincipals().isEmpty(), "principal is required");
            Asserts.isFalse(statement.getActions().isEmpty(), "action is required");
        }
    }

    boolean essentialChanged(String path, String localPolicyJSON, com.amazonaws.services.identitymanagement.model.Role remoteRole) {
        if (!path.equals(remoteRole.getPath())) return true;
        return policyChanged(localPolicyJSON, remoteRole);
    }

    private boolean policyChanged(String localPolicyJSON, com.amazonaws.services.identitymanagement.model.Role remoteRole) {
        String policyJSON = Encodings.decodeURL(remoteRole.getAssumeRolePolicyDocument());
        Optional<Policy> remotePolicy = Optional.of(Policy.fromJson(policyJSON));

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
        if (!actionMatches) return false;

        boolean effectMatches = statement1.getEffect().equals(statement2.getEffect());
        if (!effectMatches) return false;

        List<Principal> principals1 = statement1.getPrincipals();
        List<Principal> principals2 = statement2.getPrincipals();
        boolean principleMatches = principals1.size() == principals2.size()
            && principals1.stream().allMatch(principle1 -> principals2.stream().anyMatch(principal2 -> principleEquals(principle1, principal2)));
        if (!principleMatches) return false;

        return true;
    }

    private boolean principleEquals(Principal principle1, Principal principal2) {
        return principle1.getId().equals(principal2.getId())
            && principle1.getProvider().equals(principal2.getProvider());
    }

    boolean attachedPolicyChanged(List<String> managedPolicyARNs, List<String> remoteManagedPolicyARNs) {
        if (managedPolicyARNs.size() != remoteManagedPolicyARNs.size()) return true;
        return managedPolicyARNs.stream().anyMatch(localPolicy -> !remoteManagedPolicyARNs.contains(localPolicy));
    }
}
