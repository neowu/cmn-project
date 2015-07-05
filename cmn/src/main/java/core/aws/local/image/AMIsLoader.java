package core.aws.local.image;

import com.amazonaws.regions.RegionUtils;
import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.image.Images;
import core.aws.resource.image.PreBakedAMI;
import core.aws.util.Asserts;

import java.util.HashMap;
import java.util.Map;

/**
 * @author neo
 */
public class AMIsLoader implements LocalResourceLoader {
    @Override
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        String region = node.id;
        Asserts.notNull(RegionUtils.getRegion(region), "unknown region, region={}", region);
        HashMap<String, PreBakedAMI> images = new HashMap<>();

        for (Map.Entry<?, ?> entry : node.value.entrySet()) {
            String ami = (String) entry.getKey();
            images.put(ami, new PreBakedAMI(ami, (String) entry.getValue()));
        }
        resources.add(new Images(region, images));
    }
}
