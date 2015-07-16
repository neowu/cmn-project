package core.aws.resource.ec2;

/**
 * @author neo
 */
public enum InstanceState {
    STOPPED("stopped"), TERMINATED("terminated"), RUNNING("running"), SHUTTING_DOWN("shutting-down");
    public final String name;

    InstanceState(String name) {
        this.name = name;
    }

    public boolean equalsTo(com.amazonaws.services.ec2.model.InstanceState remoteState) {
        return name.equals(remoteState.getName());
    }
}
