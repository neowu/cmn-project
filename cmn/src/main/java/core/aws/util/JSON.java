package core.aws.util;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Type;

/**
 * @author neo
 */
public final class JSON {
    private static final ObjectMapper OBJECT_MAPPER = createMapper();

    private static ObjectMapper createMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setDateFormat(new ISO8601DateFormat());
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(MapperFeature.USE_WRAPPER_NAME_AS_PROPERTY_NAME, true);
        return mapper;
    }

    public static <T> T fromJSON(Type instanceType, String json) {
        try {
            JavaType javaType = OBJECT_MAPPER.getTypeFactory().constructType(instanceType);
            return OBJECT_MAPPER.readValue(json, javaType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T fromJSON(Class<T> instanceType, String json) {
        try {
            return OBJECT_MAPPER.readValue(json, instanceType);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static String toJSON(Object object) {
        try {
            return OBJECT_MAPPER.writeValueAsString(object);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
