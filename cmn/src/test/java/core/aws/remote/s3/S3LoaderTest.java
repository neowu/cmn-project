package core.aws.remote.s3;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * @author neo
 */
class S3LoaderTest {
    private static final String ENV = "env";

    S3Loader loader;

    @BeforeEach
    void createS3Loader() {
        loader = new S3Loader(null, null);
    }

    @Test
    void validEnvBuckets() {
        assertEquals("test", loader.bucketId(ENV, "env-test"));
    }

    @Test
    void invalidEnvBuckets() {
        assertNull(loader.bucketId(ENV, "test"));
        assertNull(loader.bucketId(ENV, "en"));
        assertNull(loader.bucketId(ENV, "env-"));
    }
}
