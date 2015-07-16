package core.aws.task.ec2;

import com.amazonaws.services.ec2.model.Tag;
import core.aws.env.Environment;

/**
 * @author neo
 */
public class EC2TagHelper {
    private final Environment env;

    public EC2TagHelper(Environment env) {
        this.env = env;
    }

    public Tag env() {
        return new Tag("cloud-manager:env", env.name);
    }

    public Tag name(String name) {
        return new Tag("Name", env.name + ":" + name);
    }

    public Tag resourceId(String resourceId) {
        return new Tag(prefix() + ":resource-id", resourceId);
    }

    public Tag version(int version) {
        return new Tag(prefix() + ":version", String.valueOf(version));
    }

    public Tag type(String type) {
        return new Tag(prefix() + ":type", type);
    }

    public Tag amiImageId(String imageId) {
        return new Tag(prefix() + ":ami-image-id", imageId);
    }

    public String prefix() {
        return "cloud-manager:env:" + env.name;
    }
}
