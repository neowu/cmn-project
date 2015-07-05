package core.aws.remote.s3;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author neo
 */
public class S3LoaderTest {
    private static final String ENV = "env";

    S3Loader loader;

    @Before
    public void createS3Loader() {
        loader = new S3Loader(null, null);
    }

    @Test
    public void validEnvBuckets() {
        assertEquals("test", loader.bucketId(ENV, "env-test"));
    }

    @Test
    public void invalidEnvBuckets() {
        assertNull(loader.bucketId(ENV, "test"));
        assertNull(loader.bucketId(ENV, "en"));
        assertNull(loader.bucketId(ENV, "env-"));
    }
}
