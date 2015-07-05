package core.aws.util;

import java.util.HashMap;
import java.util.Map;

/**
 * @author neo
 */
public final class Maps {
    public static <T, V> Map<T, V> newHashMap() {
        return new HashMap<>();
    }
}
