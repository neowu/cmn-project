package core.aws.resource.as;

import com.amazonaws.services.autoscaling.model.ScalingPolicy;
import com.amazonaws.services.cloudwatch.model.ComparisonOperator;
import core.aws.resource.Resource;
import core.aws.task.as.CreatePolicyTask;
import core.aws.task.as.DeletePolicyTask;
import core.aws.workflow.Tasks;

/**
 * @author neo
 */
public class AutoScalingPolicy extends Resource {
    public ScalingPolicy remotePolicy;
    public AutoScalingGroup autoScalingGroup;
    public ComparisonOperator comparisonOperator;
    public int lastMinutes;
    public double cpuUtilizationPercentage;
    public int adjustmentPercentage;

    public AutoScalingPolicy(String id) {
        super(id);
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreatePolicyTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeletePolicyTask(this));
    }
}
