package core.aws.client;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClient;
import com.amazonaws.services.sqs.model.CreateQueueRequest;
import com.amazonaws.services.sqs.model.DeleteMessageRequest;
import com.amazonaws.services.sqs.model.DeleteQueueRequest;
import com.amazonaws.services.sqs.model.ListQueuesRequest;
import com.amazonaws.services.sqs.model.Message;
import com.amazonaws.services.sqs.model.ReceiveMessageRequest;
import com.amazonaws.services.sqs.model.ReceiveMessageResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author neo
 */
public class SQS {
    final AmazonSQS sqs;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public SQS(AWSCredentialsProvider credentials, Region region) {
        sqs = new AmazonSQSClient(credentials);
        sqs.setRegion(region);
    }

    public String createQueue(String name) {
        logger.info("get or create sqs queue, name={}", name);
        return sqs.createQueue(new CreateQueueRequest(name)).getQueueUrl();
    }

    public List<Message> longPoll(String queueURL) {
        ReceiveMessageResult result = sqs.receiveMessage(new ReceiveMessageRequest(queueURL)
            .withWaitTimeSeconds(20)
            .withMaxNumberOfMessages(10));
        List<Message> messages = result.getMessages();
        logger.info("received {} message(s)", messages.size());
        return messages;
    }

    public void deleteMessage(String queueURL, String handle) {
        sqs.deleteMessage(new DeleteMessageRequest(queueURL, handle));
    }

    public List<String> listQueueURLs(String queueNamePrefix) {
        logger.info("list queue urls, namePrefix={}", queueNamePrefix);
        return sqs.listQueues(new ListQueuesRequest(queueNamePrefix)).getQueueUrls();
    }

    public void deleteQueue(String queueURL) {
        logger.info("delete queue, queue={}", queueURL);
        sqs.deleteQueue(new DeleteQueueRequest(queueURL));
    }
}
