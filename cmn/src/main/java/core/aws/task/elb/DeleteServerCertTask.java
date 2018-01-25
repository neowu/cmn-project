package core.aws.task.elb;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.elb.ServerCert;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("del-cert")
public class DeleteServerCertTask extends Task<ServerCert> {
    public DeleteServerCertTask(ServerCert serverCert) {
        super(serverCert);
    }

    @Override
    public void execute(Context context) throws Exception {
        AWS.getIam().deleteServerCert(resource.remoteCert.getServerCertificateMetadata().getServerCertificateName());
    }
}
