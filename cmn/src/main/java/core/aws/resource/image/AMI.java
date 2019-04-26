package core.aws.resource.image;

import java.util.OptionalInt;

/**
 * @author neo
 */
public interface AMI {
    String id();

    String imageId();

    OptionalInt version();
}
