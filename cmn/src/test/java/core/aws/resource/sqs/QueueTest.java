package core.aws.resource.sqs;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author neo
 */
public class QueueTest {
    @Test
    public void remoteARN() {
        Queue queue = new Queue("test");
        queue.remoteQueueURL = "https://sqs.us-east-1.amazonaws.com/167133221970/test-queue";

        String remoteARN = queue.remoteARN();

        Assert.assertEquals("arn:aws:sqs:us-east-1:167133221970:test-queue", remoteARN);
    }

    @Test
    public void remoteARNOfChinaRegion() {
        Queue queue = new Queue("test");
        queue.remoteQueueURL = "https://sqs.cn-north-1.amazonaws.com.cn/454462762806/test-queue";

        String remoteARN = queue.remoteARN();

        Assert.assertEquals("arn:aws-cn:sqs:cn-north-1:454462762806:test-queue", remoteARN);
    }
}