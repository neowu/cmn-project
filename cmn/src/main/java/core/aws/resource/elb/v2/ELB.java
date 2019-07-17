package core.aws.resource.elb.v2;

import com.amazonaws.services.elasticloadbalancingv2.model.Listener;
import com.amazonaws.services.elasticloadbalancingv2.model.LoadBalancer;
import core.aws.client.AWS;
import core.aws.resource.Resource;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.resource.elb.ServerCert;
import core.aws.resource.vpc.Subnet;
import core.aws.resource.vpc.SubnetType;
import core.aws.task.elb.v2.CreateELBListenerTask;
import core.aws.task.elb.v2.CreateELBTask;
import core.aws.task.elb.v2.DeleteELBListenerTask;
import core.aws.task.elb.v2.DeleteELBTask;
import core.aws.task.elb.v2.DescribeELBTask;
import core.aws.task.elb.v2.UpdateELBSGTask;
import core.aws.util.Asserts;
import core.aws.util.Lists;
import core.aws.workflow.Tasks;

import java.util.List;
import java.util.Optional;

public class ELB extends Resource {
    private static final int MAX_NAME_LENGTH = 32;
    public LoadBalancer remoteELB;
    public String name;
    public boolean listenHTTP;
    public boolean listenHTTPS;
    public ServerCert cert;
    public String amazonCertARN;
    public SecurityGroup securityGroup;
    public TargetGroup targetGroup;
    public List<Subnet> subnets;
    public Optional<String> scheme = Optional.empty();   // currently only allowed value is "internal"

    public ELB(String id) {
        super(id);
    }

    @Override
    public void validate(Resources resources) {
        boolean isPrivateSubnet = subnets.stream().anyMatch(subnet -> subnet.type == SubnetType.PRIVATE);
        if (isPrivateSubnet && status == ResourceStatus.LOCAL_ONLY) {
            Asserts.isFalse(scheme.isPresent(), "ELB in private subnets doesn't need scheme, it will be internal by default");
        }

        if (status == ResourceStatus.LOCAL_ONLY) {
            Asserts.isTrue(name.length() <= MAX_NAME_LENGTH, "max length of elb name is 32");
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
        if (httpsListenerRemoved()) deletedProtocols.add("HTTPS");

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

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteELBTask(this));
    }

    private boolean httpsCertChanged() {
        Optional<Listener> remoteHTTPSListener = findRemoteHTTPSListener();

        if (!listenHTTPS || remoteHTTPSListener.isEmpty()) return false;
        String remoteCertARN = remoteHTTPSListener.get().getCertificates().get(0).getCertificateArn();

        if (cert != null) {    // cert files
            if (cert.status == ResourceStatus.LOCAL_ONLY
                || !cert.remoteCert.getServerCertificateMetadata().getArn().equals(remoteCertARN))
                return true;
            return cert.changed();
        } else {
            return !remoteCertARN.equals(amazonCertARN);
        }
    }

    private boolean sgChanged() {
        if (securityGroup == null) return false;   // no vpc
        if (securityGroup.remoteSecurityGroup == null) return true;
        if (remoteELB.getSecurityGroups().isEmpty()) return true;
        return !remoteELB.getSecurityGroups().get(0).equals(securityGroup.remoteSecurityGroup.getGroupId());
    }

    private boolean httpListenerAdded() {
        return listenHTTP && !hasRemoteHTTPListener();
    }

    private boolean httpListenerRemoved() {
        return !listenHTTP && hasRemoteHTTPListener();
    }

    private boolean httpsListenerRemoved() {
        return !listenHTTPS && hasRemoteHTTPSListener();
    }

    private boolean httpsListenerAdded() {
        return listenHTTPS && !hasRemoteHTTPSListener();
    }

    private boolean hasRemoteHTTPListener() {
        List<Listener> listeners = AWS.getElbV2().listeners(remoteELB.getLoadBalancerArn());
        return listeners.stream().anyMatch(listener -> "HTTP".equals(listener.getProtocol()));
    }

    private boolean hasRemoteHTTPSListener() {
        return findRemoteHTTPSListener().isPresent();
    }

    private Optional<Listener> findRemoteHTTPSListener() {
        List<Listener> listeners = AWS.getElbV2().listeners(remoteELB.getLoadBalancerArn());
        return listeners.stream().filter(listener -> "HTTPS".equals(listener.getProtocol())).findFirst();
    }
}
