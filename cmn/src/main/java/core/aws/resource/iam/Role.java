package core.aws.resource.iam;

import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resource;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.task.iam.AttachRolePolicyTask;
import core.aws.task.iam.CreateRoleTask;
import core.aws.task.iam.DeleteRoleTask;
import core.aws.task.iam.DescribeRoleTask;
import core.aws.task.iam.DetachRolePolicyTask;
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
    public List<String> policyARNs = Lists.newArrayList();
    public String assumeRolePolicyDocument;
    public com.amazonaws.services.identitymanagement.model.Role remoteRole;
    public List<String> remoteAttachedPolicyARNs = Lists.newArrayList();

    public Role(String id) {
        super(id);
    }

    @Override
    protected void createTasks(Tasks tasks) {
        if (policyARNs.isEmpty()) {
            tasks.add(new CreateRoleTask(this));
        } else {
            CreateRoleTask createTask = tasks.add(new CreateRoleTask(this));
            AttachRolePolicyTask attachRolePolicyTask = tasks.add(new AttachRolePolicyTask(this, policyARNs));
            attachRolePolicyTask.dependsOn(createTask);
        }
    }

    @Override
    protected void updateTasks(Tasks tasks) {
        RoleHelper helper = new RoleHelper();
        if (helper.essentialChanged(path, assumeRolePolicyDocument, remoteRole)) {
            // aws role not support updating path and assume role policy doc, so we delete the role first and re-create it with new data
            DeleteRoleTask deleteRoleTask = tasks.add(new DeleteRoleTask(this));
            List<String> detachedPolicyARNs = AWS.getIam().listAttachedRolePolicyARNs(name);
            if (!detachedPolicyARNs.isEmpty()) {
                DetachRolePolicyTask detachRolePolicyTask = tasks.add(new DetachRolePolicyTask(this, detachedPolicyARNs));
                deleteRoleTask.dependsOn(detachRolePolicyTask);
            }
            CreateRoleTask createRoleTask = tasks.add(new CreateRoleTask(this));
            createRoleTask.dependsOn(deleteRoleTask);
            if (!policyARNs.isEmpty()) {
                AttachRolePolicyTask attachRolePolicyTask = tasks.add(new AttachRolePolicyTask(this, policyARNs));
                attachRolePolicyTask.dependsOn(createRoleTask);
            }
        } else {
            List<String> detachedPolicyARNs = helper.findDetachedPolicyARNs(policyARNs, remoteAttachedPolicyARNs);
            if (!detachedPolicyARNs.isEmpty()) {
                tasks.add(new DetachRolePolicyTask(this, detachedPolicyARNs));
            }
            List<String> attachedPolicyARNs = helper.findAttachedPolicyARNs(policyARNs, remoteAttachedPolicyARNs);
            if (!attachedPolicyARNs.isEmpty()) {
                tasks.add(new AttachRolePolicyTask(this, attachedPolicyARNs));
            }
        }
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        if (remoteAttachedPolicyARNs.isEmpty()) {
            tasks.add(new DeleteRoleTask(this));
        } else {
            DeleteRoleTask deleteRoleTask = tasks.add(new DeleteRoleTask(this));
            DetachRolePolicyTask detachRolePolicyTask = tasks.add(new DetachRolePolicyTask(this, remoteAttachedPolicyARNs));
            deleteRoleTask.dependsOn(detachRolePolicyTask);
        }
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeRoleTask(this));
    }

    @Override
    public void validate(Resources resources) {
        if ((status == ResourceStatus.LOCAL_ONLY || status == ResourceStatus.LOCAL_REMOTE) && Strings.notEmpty(assumeRolePolicyDocument)) {
            RoleHelper helper = new RoleHelper();
            helper.validatePolicyDocument(assumeRolePolicyDocument);
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
