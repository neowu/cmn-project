package core.aws.remote.elb;

import com.amazonaws.services.identitymanagement.model.ServerCertificateMetadata;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.elb.ServerCert;

import java.util.List;

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
        List<ServerCertificateMetadata> certs = AWS.getIam().listServerCerts(ServerCert.certPath(env));

        String prefix = env.name + "-";
        for (ServerCertificateMetadata cert : certs) {
            String certName = cert.getServerCertificateName();
            if (!certName.startsWith(prefix)) continue; // ignore cert not matching naming convention

            String resourceId = certName.substring(prefix.length());
            ServerCert serverCert = resources.find(ServerCert.class, resourceId)
                                             .orElseGet(() -> resources.add(new ServerCert(resourceId)));
            serverCert.name = certName;
            serverCert.remoteCert = AWS.getIam().getServerCert(cert.getServerCertificateName());
            serverCert.foundInRemote();
        }
    }
}