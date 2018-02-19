package core.aws.task.elb.v2;

import com.amazonaws.services.elasticloadbalancingv2.model.Tag;
import core.aws.env.Environment;

/**
 * @author neo
 */
public class ELBTagHelper {
    private final Environment env;

    public ELBTagHelper(Environment env) {
        this.env = env;
    }

    public Tag env() {
        return new Tag().withKey("cloud-manager:env").withValue(env.name);
    }

    public Tag name(String name) {
        return new Tag().withKey("Name").withValue(env.name + ":" + name);
    }

    public Tag resourceId(String resourceId) {
        return new Tag().withKey(prefix() + ":resource-id").withValue(resourceId);
    }

    public String prefix() {
        return "cloud-manager:env:" + env.name;
    }
}
