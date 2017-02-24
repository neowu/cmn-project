package core.aws.client;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClientBuilder;
import com.amazonaws.services.cloudwatch.model.DeleteAlarmsRequest;
import com.amazonaws.services.cloudwatch.model.PutMetricAlarmRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author neo
 */
public class CloudWatch {
    public final AmazonCloudWatch cloudWatch;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public CloudWatch(AWSCredentialsProvider credentials, Regions region) {
        cloudWatch = AmazonCloudWatchClientBuilder.standard().withRegion(region).withCredentials(credentials).build();
    }

    public void createAlarm(PutMetricAlarmRequest request) {
        logger.info("create cloud watch alarm, request={}", request);
        cloudWatch.putMetricAlarm(request);
    }

    public void deleteAlarms(List<String> alarmNames) {
        logger.info("delete alarms, alarms={}", alarmNames);
        cloudWatch.deleteAlarms(new DeleteAlarmsRequest().withAlarmNames(alarmNames));
    }
}
