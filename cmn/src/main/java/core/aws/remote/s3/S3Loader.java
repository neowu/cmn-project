package core.aws.remote.s3;

import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.s3.Bucket;
import core.aws.util.Asserts;

import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
public class S3Loader {
    private final Resources resources;
    private final Environment env;

    public S3Loader(Resources resources, Environment env) {
        this.resources = resources;
        this.env = env;
    }

    public void load() {
        List<com.amazonaws.services.s3.model.Bucket> buckets = AWS.s3.listAllBuckets();
        for (com.amazonaws.services.s3.model.Bucket bucket : buckets) {
            String bucketId = bucketId(env.name, bucket.getName());
            if (bucketId != null) {
                Optional<Bucket> result = resources.find(Bucket.class, bucketId);
                Bucket localBucket = result.orElseGet(() -> resources.add(new Bucket(bucketId)));
                Asserts.isNull(localBucket.remoteBucket, "the remote bucket was already loaded, please check duplicated name, bucketId={}", bucketId);
                localBucket.remoteBucket = bucket;
                localBucket.foundInRemote();
            }
        }
    }

    String bucketId(String envName, String bucketName) {
        if (!bucketName.startsWith(envName + "-")) return null;
        if (bucketName.length() <= envName.length() + 1) return null;  // ${env.name}-${name}
        return bucketName.substring(envName.length() + 1);
    }
}