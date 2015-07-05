package core.aws.remote.ec2;

import core.aws.remote.EnvTag;
import core.aws.remote.Loader;
import core.aws.resource.Resources;
import core.aws.resource.ec2.Instance;
import core.aws.resource.image.Image;
import core.aws.util.StreamHelper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author neo
 */
public class ImageLoader extends Loader {
    public ImageLoader(Resources resources, List<EnvTag> tags) {
        super(resources, tags);
    }

    @Override
    public void load() {
        all(Image.class).forEach(tag -> {
            Optional<Image> image = resources.find(Image.class, tag.resourceId());
            if (image.isPresent()) {
                image.get().foundInRemote();
                image.get().remoteImageIds.put(tag.version(), tag.remoteResourceId);
            } else {
                logger.warn("found unused image, name={}, version={}", tag.resourceId(), tag.version());
            }
        });

        // link all unfinished ami instances
        resources.stream()
            .flatMap(StreamHelper.instanceOf(Image.class))
            .forEach(image -> {
                List<Instance> instances = resources.stream()
                    .flatMap(StreamHelper.instanceOf(Instance.class))
                    .filter(instance -> instance.id.startsWith("ami-" + image.id + "-"))
                    .collect(Collectors.toList());

                image.unfinishedBakeInstances.addAll(instances);
            });
    }
}
