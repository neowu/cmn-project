package core.aws.resource.elb;

import com.amazonaws.services.elasticloadbalancing.model.ListenerDescription;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import core.aws.resource.Resource;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.resource.s3.Bucket;
import core.aws.resource.vpc.Subnet;
import core.aws.resource.vpc.SubnetType;
import core.aws.task.elb.CreateELBTask;
import core.aws.task.elb.DeleteELBTask;
import core.aws.task.elb.DescribeELBTask;
import core.aws.task.elb.UpdateELBListenerTask;
import core.aws.task.elb.UpdateELBSGTask;
import core.aws.util.Asserts;
import core.aws.workflow.Tasks;

import java.util.Optional;

/**
 * @author neo
 */
public class ELB extends Resource {
    public LoadBalancerDescription remoteELB;
    public boolean listenHTTP;
    public boolean listenHTTPS;
    public ServerCert cert;
    public String healthCheckURL;
    public SecurityGroup securityGroup;
    public Subnet subnet;
    public Bucket accessLogBucket;
    public Optional<String> scheme = Optional.empty();   // currently only allowed value is "internal"

    public ELB(String id) {
        super(id);
    }

    @Override
    public void validate(Resources resources) {
        if (status == ResourceStatus.LOCAL_ONLY && subnet.type == SubnetType.PRIVATE) {
            Asserts.isFalse(scheme.isPresent(), "ELB in private subnet doesn't need scheme, it will be internal by default");
        }
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateELBTask(this));
    }

    @Override
    protected void updateTasks(Tasks tasks) {
        if (sgChanged()) {
            tasks.add(new UpdateELBSGTask(this));
        }

        if (listenerChanged()) {
            tasks.add(new UpdateELBListenerTask(this));
        }
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeELBTask(this));
    }

    private boolean sgChanged() {
        if (securityGroup == null) return false;   // no vpc
        if (securityGroup.remoteSecurityGroup == null) return true;
        if (remoteELB.getSecurityGroups().isEmpty()) return true;
        return !remoteELB.getSecurityGroups().get(0).equals(securityGroup.remoteSecurityGroup.getGroupId());
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteELBTask(this));
    }

    public boolean httpListenerAdded() {
        return listenHTTP && !hasRemoteHTTPListener();
    }

    public boolean httpListenerRemoved() {
        return !listenHTTP && hasRemoteHTTPListener();
    }

    public boolean httpsListenerAdded() {
        Optional<ListenerDescription> remoteHTTPSListener = findRemoteHTTPSListener();
        return listenHTTPS && !remoteHTTPSListener.isPresent();
    }

    public boolean httpsListenerRemoved() {
        Optional<ListenerDescription> remoteHTTPSListener = findRemoteHTTPSListener();
        return !listenHTTPS && remoteHTTPSListener.isPresent();
    }

    public boolean httpsCertChanged() {
        Optional<ListenerDescription> remoteHTTPSListener = findRemoteHTTPSListener();

        if (listenHTTPS && remoteHTTPSListener.isPresent()) {
            String remoteCertARN = remoteHTTPSListener.get().getListener().getSSLCertificateId();
            if (cert.status == ResourceStatus.LOCAL_ONLY
                || !cert.remoteCert.getServerCertificateMetadata().getArn().equals(remoteCertARN))
                return true;
            if (cert.changed()) return true;
        }
        return false;
    }

    private boolean listenerChanged() {
        return httpListenerAdded() || httpListenerRemoved() || httpsListenerAdded() || httpsListenerRemoved() || httpsCertChanged();
    }

    private boolean hasRemoteHTTPListener() {
        return remoteELB.getListenerDescriptions().stream().anyMatch(listener -> "HTTP".equalsIgnoreCase(listener.getListener().getProtocol()));
    }

    private Optional<ListenerDescription> findRemoteHTTPSListener() {
        return remoteELB.getListenerDescriptions().stream().filter(listener -> "HTTPS".equalsIgnoreCase(listener.getListener().getProtocol())).findAny();
    }
}
