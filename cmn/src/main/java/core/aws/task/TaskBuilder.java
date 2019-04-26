package core.aws.task;

import core.aws.env.Context;
import core.aws.env.Goal;
import core.aws.env.Param;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.resource.ServerResource;
import core.aws.resource.image.Image;
import core.aws.util.Exceptions;
import core.aws.workflow.Tasks;
import org.apache.commons.compress.utils.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * @author neo
 */
public class TaskBuilder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Set<Goal> serverGoals = Sets.newHashSet(Goal.START, Goal.STOP, Goal.EXEC, Goal.PROVISION, Goal.UPLOAD, Goal.DEPLOY);
    private final Goal goal;
    private final Resources resources;
    private final Context context;

    public TaskBuilder(Goal goal, Resources resources, Context context) {
        this.goal = goal;
        this.resources = resources;
        this.context = context;
    }

    public Tasks build() {
        List<String> resourceIds = context.params(Param.RESOURCE_ID);

        Tasks tasks = new Tasks();
        if (goal == Goal.BAKE) {
            bakeAMITasks(resourceIds, tasks);
        } else if (serverGoals.contains(goal)) {
            serverTasks(resourceIds, tasks);
        } else {
            resources.stream().forEach(resource -> resource.tasks(goal, tasks));
        }
        return tasks;
    }

    private void serverTasks(List<String> resourceIds, Tasks tasks) {
        if (resourceIds != null) {
            Optional<String> invalidResourceId = resourceIds.stream().filter(resourceId -> resources.stream().noneMatch(resource -> resource.id.equals(resourceId))).findFirst();
            if (invalidResourceId.isPresent())
                throw Exceptions.error("resourceId is invalid, resourceId={}", invalidResourceId.get());
        }

        resources.stream().filter(ServerResource.class::isInstance).forEach(resource -> {
            if (resourceIds == null || resourceIds.contains(resource.id)) {
                if (resource.status == ResourceStatus.LOCAL_REMOTE) {
                    logger.info("build server task, goal={}, id={}", goal, resource.id);
                    ((ServerResource) resource).serverTasks(goal, tasks);
                } else {
                    logger.info("resource is skipped due to status, id={}, status={}", resource.id, resource.status);
                }
            }
        });
    }

    private void bakeAMITasks(List<String> resourceIds, Tasks tasks) {
        for (String resourceId : resourceIds) {
            logger.info("build bake task, imageId={}", resourceId);
            Image image = resources.get(Image.class, resourceId);
            image.bakeTasks(tasks, "true".equals(context.param(Param.RESUME_BAKE)));
        }
    }
}
