package core.aws.resource;

import com.amazonaws.regions.Regions;
import core.aws.resource.image.AMI;
import core.aws.resource.image.Image;
import core.aws.resource.image.Images;
import core.aws.resource.vpc.Subnet;
import core.aws.resource.vpc.SubnetType;
import core.aws.resource.vpc.VPC;
import core.aws.util.StreamHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * @author neo
 */
public class Resources {
    private final List<Resource> resources = new ArrayList<>();
    public final VPC vpc;

    public Resources() {
        vpc = new VPC();
        resources.add(vpc);
    }

    public <T extends Resource> Optional<T> find(Class<T> resourceClass, String resourceId) {
        return resources.stream().flatMap(StreamHelper.instanceOf(resourceClass))
            .filter(resource -> resource.id.equals(resourceId))
            .reduce(StreamHelper.onlyOne());
    }

    public <T extends Resource> T get(Class<T> resourceClass, String resourceId) {
        return find(resourceClass, resourceId)
            .orElseThrow(() -> new IllegalStateException("can not find resource, resourceClass=" + resourceClass + ", resourceId=" + resourceId));
    }

    public <T extends Resource> Optional<T> onlyOne(Class<T> resourceClass) {
        return resources.stream().flatMap(StreamHelper.instanceOf(resourceClass)).reduce(StreamHelper.onlyOne());
    }

    public <T extends Resource> T add(T resource) {
        resources.add(resource);
        return resource;
    }

    public AMI ami(Regions region, String imageId) {
        Optional<Image> image = find(Image.class, imageId);
        if (image.isPresent()) return image.get();
        return get(Images.class, region.getName()).ami(imageId);
    }

    public Optional<Subnet> firstPublicSubnet() {
        return resources.stream().flatMap(StreamHelper.instanceOf(Subnet.class))
            .filter(resource -> resource.type == SubnetType.PUBLIC).findFirst();
    }

    public void validate() {
        resources.forEach(resource -> resource.validate(this));
    }

    public Stream<Resource> stream() {
        return resources.stream();
    }
}
