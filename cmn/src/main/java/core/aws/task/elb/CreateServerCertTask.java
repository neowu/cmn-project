package core.aws.task.elb;

import com.amazonaws.services.identitymanagement.model.UploadServerCertificateRequest;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.ServerCert;
import core.aws.util.Threads;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.time.Duration;

/**
 * @author neo
 */
@Action("create-cert")
public class CreateServerCertTask extends Task<ServerCert> {
    public CreateServerCertTask(ServerCert serverCert) {
        super(serverCert);
    }

    @Override
    public void execute(Context context) throws Exception {
        resource.remoteCert = AWS.getIam().createServerCert(new UploadServerCertificateRequest()
            .withPath(ServerCert.certPath(context.env))
            .withServerCertificateName(resource.name)
            .withCertificateBody(resource.certificate)
            .withPrivateKey(resource.privateKey)
            .withCertificateChain(resource.chain));

        Threads.sleepRoughly(Duration.ofSeconds(10)); // wait 10 seconds to make cert visible to other system
    }
}
