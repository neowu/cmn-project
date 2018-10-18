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
import core.aws.task.elb.CreateELBListenerTask;
import core.aws.task.elb.CreateELBTask;
import core.aws.task.elb.DeleteELBListenerTask;
import core.aws.task.elb.DeleteELBTask;
import core.aws.task.elb.DescribeELBTask;
import core.aws.task.elb.UpdateELBSGTask;
import core.aws.util.Asserts;
import core.aws.util.Lists;
import core.aws.workflow.Tasks;

import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
public class ELB extends Resource {
    public LoadBalancerDescription remoteELB;
    public String name;
    public boolean listenHTTP;
    public boolean listenHTTPS;
    public ServerCert cert;
    public String healthCheckURL;
    public SecurityGroup securityGroup;
    public Subnet subnet;
    public Bucket accessLogBucket;
    public Optional<String> scheme = Optional.empty();   // currently only allowed value is "internal"
    public String amazonCertARN;

    public ELB(String id) {
        super(id);
    }

    @Override
    public void validate(Resources resources) {
        if (status == ResourceStatus.LOCAL_ONLY && subnet.type == SubnetType.PRIVATE) {
            Asserts.isFalse(scheme.isPresent(), "ELB in private subnet doesn't need scheme, it will be internal by default");
        }

        if (status == ResourceStatus.LOCAL_ONLY) {
            Asserts.isTrue(name.length() <= 32, "max length of elb name is 32");
            Asserts.isTrue(name.matches("[a-zA-Z0-9\\-]+"), "elb name can only contain alphanumeric, and '-'");
        }

        if (listenHTTPS && (status == ResourceStatus.LOCAL_ONLY || status == ResourceStatus.LOCAL_REMOTE)) {
            Asserts.isTrue(amazonCertARN != null || cert != null, "https listener requires cert");
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

        CreateELBListenerTask createELBListenerTask = null;
        List<String> addedProtocols = Lists.newArrayList();
        if (httpListenerAdded()) addedProtocols.add("HTTP");
        if (httpsListenerAdded() || httpsCertChanged()) addedProtocols.add("HTTPS");
        if (!addedProtocols.isEmpty()) {
            createELBListenerTask = new CreateELBListenerTask(this, addedProtocols);
            tasks.add(createELBListenerTask);
        }

        List<String> deletedProtocols = Lists.newArrayList();
        if (httpListenerRemoved()) deletedProtocols.add("HTTP");
        if (httpsListenerRemoved() || httpsCertChanged()) deletedProtocols.add("HTTPS");
        if (!deletedProtocols.isEmpty()) {
            DeleteELBListenerTask deleteELBListenerTask = new DeleteELBListenerTask(this, deletedProtocols);
            if (createELBListenerTask != null) createELBListenerTask.dependsOn(deleteELBListenerTask);
            tasks.add(deleteELBListenerTask);
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

    private boolean httpListenerAdded() {
        return listenHTTP && !hasRemoteHTTPListener();
    }

    private boolean httpListenerRemoved() {
        return !listenHTTP && hasRemoteHTTPListener();
    }

    private boolean httpsListenerAdded() {
        Optional<ListenerDescription> remoteHTTPSListener = findRemoteHTTPSListener();
        return listenHTTPS && !remoteHTTPSListener.isPresent();
    }

    private boolean httpsListenerRemoved() {
        Optional<ListenerDescription> remoteHTTPSListener = findRemoteHTTPSListener();
        return !listenHTTPS && remoteHTTPSListener.isPresent();
    }

    boolean httpsCertChanged() {
        Optional<ListenerDescription> remoteHTTPSListener = findRemoteHTTPSListener();

        if (!listenHTTPS || !remoteHTTPSListener.isPresent()) return false;
        String remoteCertARN = remoteHTTPSListener.get().getListener().getSSLCertificateId();

        if (cert != null) {    // cert files
            if (cert.status == ResourceStatus.LOCAL_ONLY
                || !cert.remoteCert.getServerCertificateMetadata().getArn().equals(remoteCertARN))
                return true;
            return cert.changed();
        } else {
            return !remoteCertARN.equals(amazonCertARN);
        }
    }

    private boolean hasRemoteHTTPListener() {
        return remoteELB.getListenerDescriptions().stream().anyMatch(listener -> "HTTP".equalsIgnoreCase(listener.getListener().getProtocol()));
    }

    private Optional<ListenerDescription> findRemoteHTTPSListener() {
        return remoteELB.getListenerDescriptions().stream().filter(listener -> "HTTPS".equalsIgnoreCase(listener.getListener().getProtocol())).findAny();
    }
}
