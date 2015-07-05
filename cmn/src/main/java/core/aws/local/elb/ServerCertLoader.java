package core.aws.local.elb;

import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.elb.ServerCert;
import core.aws.util.Files;

import java.util.Optional;

/**
 * @author neo
 */
public class ServerCertLoader implements LocalResourceLoader {
    @Override
    public void load(ResourceNode node, Resources resources, DependencyResolvers resolvers, Environment env) {
        String certPath = node.requiredString("cert");
        String privateKeyPath = node.requiredString("private-key");
        Optional<String> chainPath = node.getString("chain");

        ServerCert serverCert = resources.add(new ServerCert(node.id));
        serverCert.path = ServerCert.certPath(env);
        serverCert.name = env.name + "-" + node.id;
        serverCert.certificate = Files.text(env.envDir.resolve(certPath));
        serverCert.privateKey = Files.text(env.envDir.resolve(privateKeyPath));

        chainPath.ifPresent(path -> serverCert.chain = Files.text(env.envDir.resolve(path)));
    }
}
