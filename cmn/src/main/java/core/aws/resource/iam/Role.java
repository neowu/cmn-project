package core.aws.resource.iam;

import core.aws.env.Environment;
import core.aws.resource.Resource;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.task.iam.CreateRoleTask;
import core.aws.task.iam.DeleteRoleTask;
import core.aws.task.iam.UpdateRolePolicyTask;
import core.aws.task.iam.UpdateRoleTask;
import core.aws.util.Lists;
import core.aws.util.Strings;
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
    public List<String> managedPolicyARNs = Lists.newArrayList();
    public String assumeRolePolicyDocument;
    public com.amazonaws.services.identitymanagement.model.Role remoteRole;
    public List<String> remoteManagedPolicyARNs = Lists.newArrayList();

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
        if (helper.essentialChanged(path, assumeRolePolicyDocument, remoteRole)) {
            tasks.add(new UpdateRoleTask(this));
        } else if (helper.managedPolicyChanged(managedPolicyARNs, remoteManagedPolicyARNs)) {
            tasks.add(new UpdateRolePolicyTask(this));
        }
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteRoleTask(this));
    }

    @Override
    public void validate(Resources resources) {
        if ((status == ResourceStatus.LOCAL_ONLY || status == ResourceStatus.LOCAL_REMOTE) && Strings.notEmpty(assumeRolePolicyDocument)) {
            RoleHelper helper = new RoleHelper();
            helper.validatePolicyDocument(assumeRolePolicyDocument);
        }
    }
}
