package core.aws.client;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Principal;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.auth.policy.actions.SNSActions;
import com.amazonaws.auth.policy.actions.SQSActions;
import com.amazonaws.auth.policy.conditions.ConditionFactory;
import com.amazonaws.regions.Region;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.CreateTopicRequest;
import com.amazonaws.services.sns.model.DeleteTopicRequest;
import com.amazonaws.services.sns.model.ListSubscriptionsByTopicResult;
import com.amazonaws.services.sns.model.ListTopicsRequest;
import com.amazonaws.services.sns.model.ListTopicsResult;
import com.amazonaws.services.sns.model.SetTopicAttributesRequest;
import com.amazonaws.services.sns.model.Topic;
import core.aws.util.Lists;
import core.aws.util.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author neo
 */
public class SNS {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final AmazonSNS sns;

    public SNS(AWSCredentialsProvider credentials, Region region) {
        sns = new AmazonSNSClient(credentials);
        sns.setRegion(region);
    }

    public String createTopic(String name) {
        logger.info("get or create sns topic, name={}", name);
        String topicARN = sns.createTopic(new CreateTopicRequest(name)).getTopicArn();

        Policy policy = new Policy().withStatements(new Statement(Statement.Effect.Allow)
            .withId("TopicPolicyStatement")
            .withActions(SNSActions.Publish)
            .withResources(new Resource("*"))
            .withPrincipals(Principal.AllUsers));

        sns.setTopicAttributes(new SetTopicAttributesRequest().withTopicArn(topicARN)
            .withAttributeName("Policy")
            .withAttributeValue(policy.toJson()));

        return topicARN;
    }

    public void deleteTopic(String topicARN) {
        logger.info("delete topic, arn={}", topicARN);

        sns.deleteTopic(new DeleteTopicRequest(topicARN));
    }

    public List<Topic> listTopics() {
        logger.info("list all sns topics");

        List<Topic> topics = new ArrayList<>();
        String nextToken = null;
        while (true) {
            ListTopicsResult result = sns.listTopics(new ListTopicsRequest().withNextToken(nextToken));
            topics.addAll(result.getTopics());
            nextToken = result.getNextToken();
            if (nextToken == null) break;
        }
        return topics;
    }

    public List<String> listSubscribedQueues(String topicARN) {
        logger.info("list sqs subscriptions, topicARN={}", topicARN);

        List<String> queueURLs = Lists.newArrayList();
        String nextToken = null;
        while (true) {
            ListSubscriptionsByTopicResult result = sns.listSubscriptionsByTopic(topicARN, nextToken);

            result.getSubscriptions().forEach(subscription -> {
                if ("sqs".equals(subscription.getProtocol())) queueURLs.add(subscription.getEndpoint());
            });

            nextToken = result.getNextToken();
            if (nextToken == null) break;
        }
        return queueURLs;
    }

    public void subscribe(String topicARN, String queueURL, String queueARN) {
        logger.info("subscribe queue to topic, topicARN={}, queueURL={}", topicARN, queueURL);
        sns.subscribe(topicARN, "sqs", queueARN);

        Policy sqsPolicy = new Policy()
            .withStatements(new Statement(Statement.Effect.Allow)
                .withPrincipals(Principal.AllUsers)
                .withResources(new Resource(queueARN))
                .withConditions(ConditionFactory.newSourceArnCondition(topicARN))
                .withActions(SQSActions.SendMessage));

        Map<String, String> attributes = Maps.newHashMap();
        attributes.put("Policy", sqsPolicy.toJson());
        AWS.sqs.sqs.setQueueAttributes(queueURL, attributes);
    }
}
