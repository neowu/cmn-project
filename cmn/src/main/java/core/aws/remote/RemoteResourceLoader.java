package core.aws.remote;

import com.amazonaws.services.ec2.model.DescribeTagsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.TagDescription;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.remote.as.ASGroupLoader;
import core.aws.remote.ec2.ImageLoader;
import core.aws.remote.ec2.InstanceLoader;
import core.aws.remote.ec2.InstanceProfileLoader;
import core.aws.remote.ec2.KeyPairLoader;
import core.aws.remote.ec2.SGLoader;
import core.aws.remote.elb.ELBLoader;
import core.aws.remote.elb.ServerCertLoader;
import core.aws.remote.s3.S3Loader;
import core.aws.remote.sns.SNSLoader;
import core.aws.remote.sqs.SQSLoader;
import core.aws.remote.vpc.InternetGatewayLoader;
import core.aws.remote.vpc.NATLoader;
import core.aws.remote.vpc.RouteTableLoader;
import core.aws.remote.vpc.SubnetLoader;
import core.aws.remote.vpc.VPCLoader;
import core.aws.resource.Resources;
import core.aws.task.ec2.EC2TagHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author neo
 */
public class RemoteResourceLoader {
    public void load(Environment env, Resources resources) {
        List<EnvTag> tags = loadEnvTags(env);

        new SQSLoader(resources, env).load();
        new S3Loader(resources, env).load();
        new SNSLoader(resources, env).load();
        new InstanceProfileLoader(resources, env).load();
        new SGLoader(resources, tags).load();
        new KeyPairLoader(resources, env).load();
        new InstanceLoader(resources, tags).load();
        new VPCLoader(resources, tags).load();
        new NATLoader(resources, tags).load();
        new RouteTableLoader(resources, tags).load();
        new InternetGatewayLoader(resources, tags).load();
        new SubnetLoader(resources, tags).load();
        new ServerCertLoader(resources, env).load();
        new ELBLoader(resources, env).load();
        new ASGroupLoader(resources, env).load();
        new ImageLoader(resources, tags).load();  // TODO: image load needs to be after instance/key/sg loader for clean up, find better way?
    }

    private List<EnvTag> loadEnvTags(Environment env) {
        Map<String, EnvTag> tags = new HashMap<>();

        DescribeTagsRequest request = new DescribeTagsRequest()
            .withFilters(new Filter("key").withValues(new EC2TagHelper(env).prefix() + ":*"));
        List<TagDescription> remoteTags = AWS.ec2.describeTags(request);

        for (TagDescription remoteTag : remoteTags) {
            String remoteResourceId = remoteTag.getResourceId();
            EnvTag tag = tags.get(remoteResourceId);
            if (tag == null) {
                tag = EnvTag.newTag(remoteTag);
                tags.put(remoteResourceId, tag);
            }
            tag.addField(remoteTag);
        }

        return new ArrayList<>(tags.values());
    }
}