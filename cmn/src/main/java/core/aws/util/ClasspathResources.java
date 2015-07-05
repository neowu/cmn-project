package core.aws.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.util.Properties;

/**
 * @author neo
 */
public final class ClasspathResources {
    public static Properties properties(String path) {
        try {
            Properties properties = new Properties();
            properties.load(new StringReader(text(path)));
            return properties;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String text(String path) {
        return new String(bytes(path), Charsets.UTF_8);
    }

    public static byte[] bytes(String path) {
        try (InputStream stream = ClasspathResources.class.getClassLoader().getResourceAsStream(path)) {
            Asserts.notNull(stream, "can not load resource, path={}", path);
            return InputStreams.bytes(stream);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}

