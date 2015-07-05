package core.aws.client;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketTaggingConfiguration;
import com.amazonaws.services.s3.model.DeleteObjectsRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.s3.model.StorageClass;
import com.amazonaws.services.s3.model.TagSet;
import com.amazonaws.util.BinaryUtils;
import com.amazonaws.util.Md5Utils;
import core.aws.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * @author neo
 */
public class S3 {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    public final AmazonS3 s3;

    public S3(AWSCredentialsProvider credentials, Region region) {
        s3 = new AmazonS3Client(credentials);
        s3.setRegion(region);
    }

    public void createFolder(String bucket, String folder) {
        Asserts.isTrue(folder.startsWith("/"), "s3 key can't start with /, folder={}", folder);

        logger.info("create folder, bucket={}, folder={}", bucket, folder);
        InputStream input = new ByteArrayInputStream(new byte[0]);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(0);
        s3.putObject(new PutObjectRequest(bucket, folder, input, metadata).withStorageClass(StorageClass.ReducedRedundancy));
    }

    public void putObject(String bucket, String key, String content) {
        Asserts.isFalse(key.startsWith("/"), "s3 key can't start with /, key={}", key);

        byte[] bytes = content.getBytes(Charset.forName("UTF-8"));

        String etag = etag(bytes);
        if (etagMatches(bucket, key, etag)) return;

        logger.info("put string content, bucket={}, key={}, contentLength={}", bucket, key, bytes.length);

        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(bytes.length);
        s3.putObject(new PutObjectRequest(bucket, key, inputStream, objectMetadata).withStorageClass(StorageClass.ReducedRedundancy));
    }

    public void putObject(String bucket, String key, File file) {
        Asserts.isFalse(key.startsWith("/"), "s3 key can't start with /, key={}", key);

        String etag = etag(file);
        if (etagMatches(bucket, key, etag)) return;

        logger.info("put object, bucket={}, key={}, file={}", bucket, key, file.getAbsoluteFile());
        s3.putObject(new PutObjectRequest(bucket, key, file).withStorageClass(StorageClass.ReducedRedundancy));
    }

    public void deleteAll(String bucket) {
        logger.info("delete all from bucket, bucket={}", bucket);

        ObjectListing listing = s3.listObjects(new ListObjectsRequest().withBucketName(bucket));

        while (listing != null) {
            List<DeleteObjectsRequest.KeyVersion> keys = new ArrayList<>(listing.getObjectSummaries().size());
            for (S3ObjectSummary summary : listing.getObjectSummaries()) {
                String key = summary.getKey();
                logger.info("add key to deletion batch, key={}", key);
                keys.add(new DeleteObjectsRequest.KeyVersion(key));
            }
            if (!keys.isEmpty()) {
                logger.info("delete key batch");
                s3.deleteObjects(new DeleteObjectsRequest(bucket).withKeys(keys));
            }
            if (!listing.isTruncated()) return;

            listing = s3.listNextBatchOfObjects(listing);
        }
    }

    public void putFolder(String bucket, File folder, String prefix) {
        logger.info("put folder, bucket={}, folder={}, keyPrefix={}", bucket, folder, prefix);
        URI rootURI = folder.getParentFile().toURI();
        putFolder(bucket, folder, rootURI, prefix);
    }

    private void putFolder(String bucket, File folder, URI rootURI, String prefix) {
        File[] childFiles = folder.listFiles();
        if (childFiles != null)
            for (File childFile : childFiles) {
                if (childFile.isDirectory()) {
                    putFolder(bucket, childFile, rootURI, prefix);
                } else {
                    String key = prefix + rootURI.relativize(childFile.toURI()).toString();
                    putObject(bucket, key, childFile);
                }
            }
    }

    // use same code of aws s3 client to calculate etag, refer to AmazonS3Client.putObject()
    private String etag(File file) {
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            byte[] md5Hash = Md5Utils.computeMD5Hash(fileInputStream);
            return BinaryUtils.toHex(md5Hash);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private String etag(byte[] bytes) {
        byte[] md5Hash = Md5Utils.computeMD5Hash(bytes);
        return BinaryUtils.toHex(md5Hash);
    }

    private boolean etagMatches(String bucket, String key, String etag) {
        try {
            S3Object object = s3.getObject(bucket, key);
            if (object != null) {
                String existingETag = object.getObjectMetadata().getETag();
                if (etag.equals(existingETag)) {
                    logger.info("etag matches, skip uploading, bucket={}, key={}", bucket, key);
                    return true;
                }
            }
        } catch (AmazonS3Exception e) {
            if (!"NoSuchKey".equals(e.getErrorCode())) {
                throw e;
            }
        }
        return false;
    }

    public List<Bucket> listAllBuckets() {
        logger.info("list all s3 buckets");
        return s3.listBuckets();
    }

    public Bucket createBucket(String bucketName) {
        logger.info("create s3 bucket, name={}", bucketName);
        return s3.createBucket(bucketName);
    }

    public void deleteBucket(String bucketName) {
        logger.info("delete s3 bucket, name={}", bucketName);
        s3.deleteBucket(bucketName);
    }

    public void createTags(String bucketName, TagSet tags) {
        logger.info("tag s3 bucket, bucketName={}", bucketName);
        s3.setBucketTaggingConfiguration(bucketName, new BucketTaggingConfiguration().withTagSets(tags));
    }
}
