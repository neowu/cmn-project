package core.aws.resource.image;

import core.aws.util.ToStringHelper;

import java.util.OptionalInt;

/**
 * @author neo
 */
public class PreBakedAMI implements AMI {
    private final String id;
    private final String imageId;

    public PreBakedAMI(String id, String imageId) {
        this.id = id;
        this.imageId = imageId;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String imageId() {
        return imageId;
    }

    @Override
    public OptionalInt version() {
        return OptionalInt.empty();
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(id)
            .add("imageId", imageId)
            .toString();
    }
}
