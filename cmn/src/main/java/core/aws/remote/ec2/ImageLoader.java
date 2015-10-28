package core.aws.remote.ec2;

import core.aws.client.AWS;
import core.aws.remote.EnvTag;
import core.aws.remote.Loader;
import core.aws.resource.Resources;
import core.aws.resource.image.Image;
import core.aws.util.Maps;

import java.util.List;
import java.util.Map;
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
        Map<String, com.amazonaws.services.ec2.model.Image> remoteImages = loadRemoteImages();

        all(Image.class).forEach(tag -> {
            Optional<Image> image = resources.find(Image.class, tag.resourceId());
            if (image.isPresent()) {
                image.get().foundInRemote();

                com.amazonaws.services.ec2.model.Image remoteImage = remoteImages.get(tag.remoteResourceId);
                if ("available".equals(remoteImage.getState()))
                    image.get().remoteImages.put(tag.version(), remoteImage);
            } else {
                logger.warn("found unused image, name={}, version={}", tag.resourceId(), tag.version());
            }
        });
    }

    private Map<String, com.amazonaws.services.ec2.model.Image> loadRemoteImages() {
        Map<String, com.amazonaws.services.ec2.model.Image> remoteImages = Maps.newHashMap();
        List<String> remoteImageIds = all(Image.class).map(tag -> tag.remoteResourceId).collect(Collectors.toList());
        if (!remoteImageIds.isEmpty()) {    // check if the env has any remote image
            List<com.amazonaws.services.ec2.model.Image> images = AWS.ec2.describeImages(remoteImageIds);
            for (com.amazonaws.services.ec2.model.Image image : images) {
                remoteImages.put(image.getImageId(), image);
            }
        }
        return remoteImages;
    }
}
