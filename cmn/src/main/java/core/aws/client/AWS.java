package core.aws.client;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.auth.PropertiesCredentials;
import core.aws.env.Environment;
import core.aws.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * due to nature of cloud manager, to use plain design to achieve better simplicity and flexibility
 *
 * @author neo
 */
public class AWS {
    public static final Logger LOGGER = LoggerFactory.getLogger(AWS.class);
    public static EC2 ec2;
    public static S3 s3;
    public static EC2VPC vpc;
    public static ElasticLoadBalancing elb;
    public static IAM iam;
    public static AutoScaling as;
    public static CloudWatch cloudWatch;

    public static void initialize(Environment env) throws IOException {
        Asserts.isNull(ec2, "initialize should only be called once");

        LOGGER.info("initialize aws clients");
        AWSCredentialsProvider provider = loadAWSCredentials(env.envDir);
        ec2 = new EC2(provider, env.region);
        vpc = new EC2VPC(provider, env.region);
        elb = new ElasticLoadBalancing(provider, env.region);
        s3 = new S3(provider, env.region);
        iam = new IAM(provider, env.region);
        as = new AutoScaling(provider, env.region);
        cloudWatch = new CloudWatch(provider, env.region);
    }

    private static AWSCredentialsProvider loadAWSCredentials(Path envDir) throws IOException {
        AWSCredentialsProvider provider;
        Path awsCredential = envDir.resolve("aws.properties");
        if (Files.exists(awsCredential)) {
            LOGGER.info("found aws.properties, use it as aws credentials, file={}", awsCredential);
            PropertiesCredentials credentials = new PropertiesCredentials(awsCredential.toFile());
            provider = new AWSStaticCredentialsProvider(credentials);
        } else {
            LOGGER.info("not found aws.properties, use default credentials (env or instanceProfile)");
            provider = new DefaultAWSCredentialsProviderChain();
        }
        return provider;
    }
}
