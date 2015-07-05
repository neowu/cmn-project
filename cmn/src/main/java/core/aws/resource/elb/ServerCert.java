package core.aws.resource.elb;

import com.amazonaws.services.identitymanagement.model.ServerCertificate;
import core.aws.resource.Resource;
import core.aws.task.elb.CreateServerCertTask;
import core.aws.task.elb.DeleteServerCertTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class ServerCert extends Resource {
    public String name;
    public String certificate;
    public String privateKey;
    public String chain;
    public ServerCertificate remoteCert;

    public ServerCert(String id) {
        super(id);
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateServerCertTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteServerCertTask(this));
    }

    @Override
    protected void updateTasks(Tasks tasks) {
        if (changed()) {
            DeleteServerCertTask deleteTask = tasks.add(new DeleteServerCertTask(this));
            CreateServerCertTask createTask = tasks.add(new CreateServerCertTask(this));
            createTask.dependsOn(deleteTask);
        }
    }

    public boolean changed() {
        String remoteCertBody = normalize(remoteCert.getCertificateBody());
        String localCertBody = normalize(certificate);
        return !remoteCertBody.equals(localCertBody);
    }

    private String normalize(String cert) {
        // clean windows/unix line separator and spaces
        return cert.replace("\r", "").replace("\n", "").replace(" ", "");
    }
}
