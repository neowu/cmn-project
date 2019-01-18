package core.aws.local;

import core.aws.util.ClasspathResources;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author mort
 */
class ResourcesLoaderTest {

    @Test
    void load() {
        ResourcesLoader loader = new ResourcesLoader();
        Path path = Paths.get("/");
        String yml = ClasspathResources.text("01-eks.yml");
        List<ResourceNode> resourceNodes = loader.load(yml, path);
        assertFalse(resourceNodes.isEmpty());
    }
}