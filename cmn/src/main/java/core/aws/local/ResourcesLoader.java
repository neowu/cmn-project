package core.aws.local;

import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.local.as.ASGroupLoader;
import core.aws.local.ec2.InstanceLoader;
import core.aws.local.ec2.InstanceProfileLoader;
import core.aws.local.ec2.SGLoader;
import core.aws.local.elb.ELBLoader;
import core.aws.local.elb.ServerCertLoader;
import core.aws.local.elb.v2.TargetGroupLoader;
import core.aws.local.env.EnvLoader;
import core.aws.local.iam.RoleLoader;
import core.aws.local.image.AMILoader;
import core.aws.local.image.AMIsLoader;
import core.aws.local.s3.S3Loader;
import core.aws.local.vpc.SubnetLoader;
import core.aws.resource.Resource;
import core.aws.resource.Resources;
import core.aws.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author neo
 */
public class ResourcesLoader {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Map<String, LocalResourceLoader> localResourceLoaders = new HashMap<>();
    private final Pattern resourceNamePattern = Pattern.compile("(.*)\\[(.*)\\]");

    public ResourcesLoader() {
        localResourceLoaders.put("amis", new AMIsLoader());
        localResourceLoaders.put("ami", new AMILoader());
        localResourceLoaders.put("security-group", new SGLoader());
        localResourceLoaders.put("instance", new InstanceLoader());
        localResourceLoaders.put("s3", new S3Loader());
        localResourceLoaders.put("subnet", new SubnetLoader());
        localResourceLoaders.put("cert", new ServerCertLoader());
        localResourceLoaders.put("target-group", new TargetGroupLoader());
        localResourceLoaders.put("elb", new ELBLoader());
        localResourceLoaders.put("elb-v2", new core.aws.local.elb.v2.ELBLoader());
        localResourceLoaders.put("instance-profile", new InstanceProfileLoader());
        localResourceLoaders.put("auto-scaling", new ASGroupLoader());
        localResourceLoaders.put("iam-role", new RoleLoader());
    }

    public Resources load(Environment env) throws IOException {
        Resources resources = new Resources();
        DependencyResolvers resolvers = new DependencyResolvers();

        List<ResourceNode> nodes = new ArrayList<>();

        Files.list(env.envDir)
            .filter(path -> path.getFileName().toString().endsWith(".yml"))
            .forEach(path -> {
                logger.info("load env config, file={}", path);
                String yml = core.aws.util.Files.text(path);
                nodes.addAll(load(yml, path));
            });

        nodes.stream()
            .filter(node -> "env".equals(node.type))
            .forEach(node -> new EnvLoader().load(node, env));

        AWS.initialize(env);    //TODO: AWS clients need to be init after env.region is determined, refactor this into more clear flow

        nodes.stream()
            .filter(node -> !"env".equals(node.type))
            .forEach(node -> {
                LocalResourceLoader loader = Asserts.notNull(localResourceLoaders.get(node.type), "unknown resource type, type={}", node.type);
                try {
                    loader.load(node, resources, resolvers, env);
                } catch (Exception e) {
                    logger.error("failed to parse resource node, file={}, node=\n{}", node.path, node.yml);
                    throw e;
                }
            });

        resolvers.resolve();
        resources.stream().forEach(Resource::foundInLocal);
        return resources;
    }

    List<ResourceNode> load(String config, Path path) {
        List<ResourceNode> results = new ArrayList<>();

        Yaml yaml = new Yaml();
        Object nodes = yaml.load(config);
        if (!(nodes instanceof List<?>)) throw new IllegalArgumentException("config must be in list, config=" + nodes);
        for (Object node : (List<?>) nodes) {
            if (!(node instanceof Map<?, ?>))
                throw new IllegalArgumentException("config node must be in map, node=" + node);
            Map<?, ?> map = (Map<?, ?>) node;
            if (map.size() > 1) throw new IllegalArgumentException("invalid node format, node=" + node);
            String name = String.valueOf(map.keySet().iterator().next());
            Matcher matcher = resourceNamePattern.matcher(name);
            if (matcher.matches()) {
                String resourceType = matcher.group(1);
                String resourceId = matcher.group(2);
                ResourceNode result = new ResourceNode(resourceType, resourceId, (Map<?, ?>) map.get(name), yaml.dump(node), path);
                results.add(result);
            } else {
                throw new IllegalArgumentException("invalid resource name format, name=" + name);
            }
        }

        return results;
    }
}
