package core.aws.remote.elb;

import com.amazonaws.services.identitymanagement.model.ServerCertificateMetadata;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.elb.ServerCert;

import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
public class ServerCertLoader {
    private final Resources resources;
    private final Environment env;

    public ServerCertLoader(Resources resources, Environment env) {
        this.resources = resources;
        this.env = env;
    }

    public void load() {
        List<ServerCertificateMetadata> certs = AWS.iam.listServerCerts(ServerCert.certPath(env));

        String prefix = env.name + "-";
        for (ServerCertificateMetadata cert : certs) {
            String certName = cert.getServerCertificateName();
            if (!certName.startsWith(prefix)) continue; // ignore cert not matching naming convention

            String resourceId = certName.substring(prefix.length());
            Optional<ServerCert> result = resources.find(ServerCert.class, resourceId);
            ServerCert serverCert = result.isPresent() ? result.get() : resources.add(new ServerCert(resourceId));
            serverCert.name = certName;
            serverCert.remoteCert = AWS.iam.getServerCert(cert.getServerCertificateName());
            serverCert.foundInRemote();
        }
    }

    // previously we use env.name.replaceAll("-", "")) to generate prefix, which is not necessary, to keep backward compatible, wider prefix
    private String prefix() {
        int index = env.name.indexOf('-');
        if (index > 0) return "/" + env.name.substring(0, index);
        return "/" + env.name + "/";
    }
}