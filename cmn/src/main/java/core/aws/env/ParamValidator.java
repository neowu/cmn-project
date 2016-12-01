package core.aws.env;

import core.aws.util.Asserts;
import core.aws.util.Lists;

import java.util.List;
import java.util.Map;

/**
 * @author neo
 */
public class ParamValidator {
    public void validate(Goal goal, Map<Param, List<String>> params) {
        if (goal == Goal.SYNC || goal == Goal.DEL) {
            validateParams(params, Lists.newArrayList(Param.ENV_PATH, Param.DRY_RUN), null);
        } else if (goal == Goal.DESC) {
            validateParams(params, Lists.newArrayList(Param.ENV_PATH), null);
        } else if (goal == Goal.BAKE) {
            validateParams(params, Lists.newArrayList(Param.ENV_PATH, Param.RESOURCE_ID, Param.RESUME_BAKE), Lists.newArrayList(Param.RESOURCE_ID));
        } else if (goal == Goal.DEPLOY || goal == Goal.STOP || goal == Goal.START) {
            validateParams(params, Lists.newArrayList(Param.ENV_PATH, Param.RESOURCE_ID), null);
        } else if (goal == Goal.EXEC) {
            validateParams(params, Lists.newArrayList(Param.ENV_PATH, Param.RESOURCE_ID, Param.INSTANCE_INDEX, Param.EXECUTE_COMMAND, Param.EXECUTE_SCRIPT), Lists.newArrayList(Param.RESOURCE_ID, Param.EXECUTE_COMMAND));
        } else if (goal == Goal.UPLOAD) {
            validateParams(params, Lists.newArrayList(Param.ENV_PATH, Param.RESOURCE_ID, Param.PACKAGE_DIR, Param.INSTANCE_INDEX), Lists.newArrayList(Param.PACKAGE_DIR, Param.RESOURCE_ID));
        } else if (goal == Goal.PROVISION) {
            validateParams(params, Lists.newArrayList(Param.ENV_PATH, Param.RESOURCE_ID, Param.PACKAGE_DIR, Param.INSTANCE_INDEX, Param.PROVISION_PLAYBOOK), Lists.newArrayList(Param.RESOURCE_ID));
        } else if (goal == Goal.SSH) {
            validateParams(params, Lists.newArrayList(Param.ENV_PATH, Param.RESOURCE_ID, Param.INSTANCE_INDEX, Param.SSH_TUNNEL_RESOURCE_ID), Lists.newArrayList(Param.RESOURCE_ID));
        } else {
            Asserts.fail("unknown goal, goal={}", goal);
        }
    }

    private void validateParams(Map<Param, List<String>> params, List<Param> allowedParams, List<Param> requiredParams) {
        boolean allParamAllowed = params.keySet().stream().allMatch(allowedParams::contains);
        Asserts.isTrue(allParamAllowed, "not allowed param found, allowed={}, input={}", allowedParams, params);

        if (requiredParams != null) {
            boolean allRequiredParamPresent = requiredParams.stream().allMatch(params::containsKey);
            Asserts.isTrue(allRequiredParamPresent, "required param is missing, required={}, input={}", requiredParams, params);
        }
    }
}
