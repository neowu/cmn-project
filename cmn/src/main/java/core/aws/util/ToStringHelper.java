package core.aws.util;

/**
 * @author neo
 */
@SuppressWarnings("PMD")
public final class ToStringHelper {
    StringBuilder builder;
    boolean first = true;

    public ToStringHelper(Object object) {
        builder = new StringBuilder(32).append(object.getClass().getSimpleName()).append('{');
    }

    public ToStringHelper add(String field, Object value) {
        return add(field + '=' + value);
    }

    public ToStringHelper add(Object value) {
        if (!first) builder.append(", ");
        builder.append(value);
        first = false;
        return this;
    }

    public ToStringHelper addIfNotNull(Object value) {
        if (value != null) add(value);
        return this;
    }

    public ToStringHelper addIfNotNull(String field, Object value) {
        if (value != null) add(field, value);
        return this;
    }

    @Override
    public String toString() {
        String result = builder.append('}').toString();
        builder = null; // one ToStringHelper can only be used once
        return result;
    }
}
