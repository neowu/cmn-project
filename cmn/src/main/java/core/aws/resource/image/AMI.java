package core.aws.resource.image;

import java.util.Optional;

/**
 * @author neo
 */
public interface AMI {
    String id();

    String imageId();

    Optional<Integer> version();
}
