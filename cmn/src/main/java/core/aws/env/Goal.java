package core.aws.env;

/**
 * @author neo
 */
public enum Goal {
    SYNC, DEL, DESC, BAKE, DEPLOY, START, STOP, EXEC, UPLOAD, PROVISION, SSH;

    public static Goal parse(String value) {
        for (Goal goal : Goal.values()) {
            if (goal.name().equalsIgnoreCase(value)) return goal;
        }
        throw new IllegalArgumentException("unknown goal, value=" + value);
    }
}
