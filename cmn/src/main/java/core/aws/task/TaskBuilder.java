package core.aws.task;

import core.aws.env.Context;
import core.aws.env.Goal;
import core.aws.env.Param;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.resource.ServerResource;
import core.aws.resource.image.Image;
import core.aws.util.Lists;
import core.aws.util.StreamHelper;
import core.aws.workflow.Tasks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author neo
 */
public class TaskBuilder {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final List<Goal> serverGoals = Lists.newArrayList(Goal.START, Goal.STOP, Goal.EXEC, Goal.PROVISION, Goal.UPLOAD, Goal.DEPLOY);
    private final Goal goal;
    private final Resources resources;
    private final Context context;

    public TaskBuilder(Goal goal, Resources resources, Context context) {
        this.goal = goal;
        this.resources = resources;
        this.context = context;
    }

    public Tasks build() {
        List<String> resourceIds = resourceIds(context.param(Param.RESOURCE_ID));

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

    List<String> resourceIds(String resourceIdParam) {
        List<String> resourceIds = null;
        if (resourceIdParam != null) resourceIds = Arrays.asList(resourceIdParam.split(","));
        return resourceIds;
    }

    private void serverTasks(List<String> resourceIds, Tasks tasks) {
        resources.stream()
            .filter(resource -> resource.status == ResourceStatus.LOCAL_REMOTE
                && (resourceIds == null || resourceIds.contains(resource.id)))
            .flatMap(StreamHelper.instanceOf(ServerResource.class))
            .forEach(resource -> resource.serverTasks(goal, tasks));
    }

    private void bakeAMITasks(List<String> resourceIds, Tasks tasks) {
        for (String resourceId : resourceIds) {
            logger.info("build bake task, imageId={}", resourceId);
            Image image = resources.get(Image.class, resourceId);
            image.bakeTasks(tasks, "true".equals(context.param(Param.RESUME_BAKE)));
        }
    }
}
