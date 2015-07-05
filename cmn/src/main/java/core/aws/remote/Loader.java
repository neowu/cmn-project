package core.aws.remote;

import core.aws.resource.Resource;
import core.aws.resource.Resources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Stream;

/**
 * @author neo
 */
public abstract class Loader {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final Resources resources;
    protected final List<EnvTag> tags;

    public Loader(Resources resources, List<EnvTag> tags) {
        this.resources = resources;
        this.tags = tags;
    }

    public Stream<EnvTag> all(final Class<? extends Resource> resourceClass) {
        return tags.stream().filter(tag -> tag.resourceClass.equals(resourceClass));
    }

    public abstract void load();
}
