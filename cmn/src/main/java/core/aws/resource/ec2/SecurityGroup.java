package core.aws.resource.ec2;

import com.amazonaws.services.ec2.model.IpPermission;
import core.aws.resource.Resource;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.resource.vpc.VPC;
import core.aws.task.ec2.CreateSGRuleTask;
import core.aws.task.ec2.CreateSGTask;
import core.aws.task.ec2.DeleteSGRuleTask;
import core.aws.task.ec2.DeleteSGTask;
import core.aws.task.ec2.DescribeSGTask;
import core.aws.util.Asserts;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * @author neo
 */
public class SecurityGroup extends Resource {
    public com.amazonaws.services.ec2.model.SecurityGroup remoteSecurityGroup;

    public String name;
    private final Map<Protocol, List<Source>> ingressRules = new HashMap<>();
    public VPC vpc;

    public SecurityGroup(String id) {
        super(id);
    }

    @Override
    public void validate(Resources resources) {
        if (status == ResourceStatus.LOCAL_REMOTE) {
            Asserts.equals(vpc.status, ResourceStatus.LOCAL_REMOTE, "inconsistent state, security group refers to vpc not created");
            Asserts.isTrue(vpc.remoteVPC.getVpcId().equals(remoteSecurityGroup.getVpcId()), "sg is in different vpc");
        }

        if (status == ResourceStatus.LOCAL_ONLY) {
            Asserts.isTrue(name.length() <= 255, "max length of sg name is 255");
            Asserts.isTrue(name.matches("[a-zA-Z0-9\\-\\:]+"), "sg name can only contain alphanumeric, '-' and ':'");
        }
    }

    @Override
    protected void createTasks(Tasks tasks) {
        CreateSGTask createTask = tasks.add(new CreateSGTask(this));
        CreateSGRuleTask createRuleTask = tasks.add(new CreateSGRuleTask(this, ingressRules));
        createRuleTask.dependsOn(createTask);
    }

    @Override
    protected void updateTasks(Tasks tasks) {
        SecurityGroupRuleHelper helper = new SecurityGroupRuleHelper(ingressRules, remoteSecurityGroup.getIpPermissions());

        List<IpPermission> deletedIngressRules = helper.findDeletedIngressRules();
        if (!deletedIngressRules.isEmpty()) {
            tasks.add(new DeleteSGRuleTask(this, deletedIngressRules));
        }

        Map<Protocol, List<Source>> addedIngressRules = helper.findAddedIngressRules();
        if (!addedIngressRules.isEmpty()) {
            tasks.add(new CreateSGRuleTask(this, addedIngressRules));
        }
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeSGTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        DeleteSGTask deleteTask = tasks.add(new DeleteSGTask(this));
        DeleteSGRuleTask deleteRuleTask = tasks.add(new DeleteSGRuleTask(this, remoteSecurityGroup.getIpPermissions()));
        deleteTask.dependsOn(deleteRuleTask);
    }

    public void addIngressRule(Protocol protocol, Source source) {
        ingressRules.computeIfAbsent(protocol, key -> new ArrayList<>())
            .add(source);
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(id)
            .add(status)
            .add("name", name)
            .toString();
    }

    public static class Source {
        public SecurityGroup securityGroup;
        public String ipRange;

        @Override
        public String toString() {
            return new ToStringHelper(this)
                .addIfNotNull(securityGroup == null ? null : securityGroup.id)
                .addIfNotNull(ipRange)
                .toString();
        }
    }
}
