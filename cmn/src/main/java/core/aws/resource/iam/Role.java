package core.aws.resource.iam;

import core.aws.env.Environment;
import core.aws.resource.Resource;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.resource.ec2.InstanceProfile;
import core.aws.task.iam.CreateRoleTask;
import core.aws.task.iam.DeleteRoleTask;
import core.aws.task.iam.DescribeRoleTask;
import core.aws.task.iam.UpdateRoleTask;
import core.aws.util.Lists;
import core.aws.util.Strings;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Tasks;

import java.util.List;

/**
 * @author mort
 */
public class Role extends Resource {
    public static String defaultPath(Environment env) {
        return String.format("/%s/", env.name.replaceAll("-", ""));
    }

    public String name;
    public String path;
    public String policy;
    public String assumeRolePolicy;
    public List<String> policyARNs = Lists.newArrayList();
    public InstanceProfile instanceProfile;

    public com.amazonaws.services.identitymanagement.model.Role remoteRole;
    public List<String> remoteAttachedPolicyARNs = Lists.newArrayList();

    public Role(String id) {
        super(id);
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateRoleTask(this));
    }

    @Override
    protected void updateTasks(Tasks tasks) {
        RoleHelper helper = new RoleHelper();
        tasks.add(new UpdateRoleTask(this,
            new UpdateRoleTask.Request()
                .essentialChanged(helper.essentialChanged(path, assumeRolePolicy, remoteRole))
                .policyChanged(helper.policyChanged(policy, remoteRole))
                .attachedPolicyARNs(helper.findAttachedPolicyARNs(policyARNs, remoteAttachedPolicyARNs))
                .detachedPolicyARNs(helper.findDetachedPolicyARNs(policyARNs, remoteAttachedPolicyARNs))));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteRoleTask(this));
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeRoleTask(this));
    }

    @Override
    public void validate(Resources resources) {
        if ((status == ResourceStatus.LOCAL_ONLY || status == ResourceStatus.LOCAL_REMOTE) && (Strings.notEmpty(assumeRolePolicy) || Strings.notEmpty(policy))) {
            RoleHelper helper = new RoleHelper();
            if (Strings.notEmpty(assumeRolePolicy)) {
                helper.validatePolicyDocument(assumeRolePolicy);
            }
            if (Strings.notEmpty(policy)) {
                helper.validatePolicyDocument(policy);
            }
        }
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(id)
            .add(status)
            .add("path", path)
            .toString();
    }
}
