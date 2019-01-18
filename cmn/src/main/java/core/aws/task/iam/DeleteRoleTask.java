package core.aws.task.iam;

import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.iam.Role;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author mort
 */
@Action("del-iam-role")
public class DeleteRoleTask extends Task<Role> {
    private final Logger logger = LoggerFactory.getLogger(DeleteRoleTask.class);

    public DeleteRoleTask(Role resource) {
        super(resource);
    }

    @Override
    public void execute(Context context) throws Exception {
        String name = resource.remoteRole.getRoleName();
        logger.info("delete role and attached policy, name={}", name);
        AWS.getIam().deleteRole(name, resource.remoteRole.getPath());
    }
}
