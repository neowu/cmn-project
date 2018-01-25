package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import com.amazonaws.services.cloudwatch.model.Statistic;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.as.AutoScalingPolicy;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("create-policy")
public class CreatePolicyTask extends Task<AutoScalingPolicy> {
    public CreatePolicyTask(AutoScalingPolicy policy) {
        super(policy);
    }

    @Override
    public void execute(Context context) throws Exception {
        String asGroupName = resource.asGroup.remoteASGroup.getAutoScalingGroupName();

        String policyARN = AWS.getAs().createPolicy(new PutScalingPolicyRequest()
            .withPolicyName(resource.id)
            .withAutoScalingGroupName(asGroupName)
            .withScalingAdjustment(resource.adjustmentPercentage)
            .withAdjustmentType("PercentChangeInCapacity")
            .withMinAdjustmentStep(1)
            .withCooldown(300));

        AWS.getCloudWatch().createAlarm(new PutMetricAlarmRequest()
            .withAlarmName(context.env.name + ":" + resource.id + "-alarm")
            .withMetricName("CPUUtilization")
            .withComparisonOperator(resource.comparisonOperator)
            .withThreshold(resource.cpuUtilizationPercentage)
            .withPeriod(60)
            .withEvaluationPeriods(resource.lastMinutes)
            .withStatistic(Statistic.Average)
            .withNamespace("AWS/EC2")
            .withDimensions(new Dimension().withName("AutoScalingGroupName").withValue(asGroupName))
            .withAlarmActions(policyARN));
    }
}
