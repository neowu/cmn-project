package core.aws.env;

/**
 * @author neo
 */
public enum Param {
    ENV_PATH("env"),
    RESOURCE_ID("id"),
    EXECUTE_COMMAND("cmd"),
    EXECUTE_SCRIPT("script"),
    PACKAGE_DIR("package-dir"),
    INSTANCE_INDEX("i"),
    PROVISION_PLAYBOOK("playbook"),
    RESUME_BAKE("resume-bake"),
    DRY_RUN("dry-run");

    public final String key;

    Param(String key) {
        this.key = key;
    }

    public static Param parse(String key) {
        Param[] params = Param.values();
        for (Param param : params) {
            if (param.key.equals(key)) return param;
        }
        throw new IllegalArgumentException("not valid param, key=" + key);
    }
}
