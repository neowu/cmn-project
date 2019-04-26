package core.aws.local;

import core.aws.util.Asserts;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * @author neo
 */
public class ResourceNode {
    public final String type;
    public final String id;
    public final Map<?, ?> value;
    public final String yml;
    public final Path path;

    public ResourceNode(String type, String id, Map<?, ?> value, String yml, Path path) {
        this.type = type;
        this.id = id;
        this.value = value;
        this.yml = yml;
        this.path = path;
    }

    public Object field(String field) {
        return value.get(field);
    }

    public Optional<String> getString(String field) {
        Object result = field(field);
        if (result == null) return Optional.empty();
        Asserts.isTrue(result instanceof String, "value is not text, field={}, value={}", field, result);
        return Optional.of((String) result);
    }

    public OptionalInt getInt(String field) {
        Object result = field(field);
        if (result == null) return OptionalInt.empty();
        Asserts.isTrue(result instanceof Integer, "value is not int, field={}, value={}", field, result);
        return OptionalInt.of((Integer) result);
    }

    public String requiredString(String field) {
        return getString(field).orElseThrow(() -> new Error("field is required, field=" + field));
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> mapField(String field) {
        Object result = field(field);
        if (result == null) return null;
        Asserts.isTrue(result instanceof Map, "value is not map, field={}, value={}", field, result);
        return (Map<String, Object>) result;
    }

    @SuppressWarnings("unchecked")
    public List<?> listField(String field) {
        Object result = field(field);
        if (result == null) return null;
        Asserts.isTrue(result instanceof List, "value is not list, field={}, value={}", field, result);
        return (List<?>) result;
    }
}
