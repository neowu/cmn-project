package core.aws.util;

import java.util.Objects;

/**
 * @author neo
 */
public final class Asserts {
    public static <T> T notNull(T reference, String message, Object... params) {
        if (reference == null) throw new AssertionError(Strings.format(message, params));
        return reference;
    }

    public static void isNull(Object reference, String message, Object... params) {
        if (reference != null) throw new AssertionError(Strings.format(message, params));
    }

    public static void isTrue(boolean condition, String message, Object... params) {
        if (!condition) throw new AssertionError(Strings.format(message, params));
    }

    public static void isFalse(boolean condition, String message, Object... params) {
        if (condition) throw new AssertionError(Strings.format(message, params));
    }

    public static <T> String notEmpty(String text, String message, Object... params) {
        if (!Strings.notEmpty(text)) throw new AssertionError(Strings.format(message, params));
        return text;
    }

    @SuppressWarnings("PMD.SuspiciousEqualsMethodName")
    public static <T> void equals(T object1, T object2, String message, Object... params) {
        if (!Objects.equals(object1, object2)) throw new AssertionError(Strings.format(message, params));
    }

    public static <T> void fail(String message, Object... params) {
        throw new AssertionError(Strings.format(message, params));
    }
}
