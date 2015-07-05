package core.aws.resource.image;

import core.aws.resource.Resource;
import core.aws.util.Asserts;

import java.util.Map;

/**
 * @author neo
 */
public class Images extends Resource {
    private final Map<String, PreBakedAMI> images;

    public Images(String id, Map<String, PreBakedAMI> images) {
        super(id);
        this.images = images;
    }

    public PreBakedAMI ami(String id) {
        return Asserts.notNull(images.get(id), "can not find ami, id={}", id);
    }
}
