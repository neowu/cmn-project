package core.aws.util;

import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * @author neo
 */
public final class StreamHelper {
    public static <T> BinaryOperator<T> onlyOne() {
        return (item1, item2) -> {
            if (item2 != null) throw new IllegalStateException("multiple items found, items=" + item1 + ", " + item2);
            return item1;
        };
    }

    @SuppressWarnings("unchecked")
    public static <T, R> Function<T, Stream<R>> instanceOf(Class<R> targetClass) {
        return item -> targetClass.isInstance(item) ? Stream.of((R) item) : Stream.empty();
    }
}
