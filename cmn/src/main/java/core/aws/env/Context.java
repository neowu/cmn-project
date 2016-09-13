package core.aws.env;

import core.aws.util.Asserts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author neo
 */
public class Context {
    final Map<Param, String> params = new ConcurrentHashMap<>();
    private final Logger messageLogger = LoggerFactory.getLogger("message");
    private final Map<String, List<String>> newOutputs = new TreeMap<>();
    private final ReentrantLock lock = new ReentrantLock();
    public Goal goal;
    public Environment env;

    public void output(String key, Object value) {
        lock.lock();
        try {
            newOutputs.computeIfAbsent(key, k -> new ArrayList<>())
                .add(String.valueOf(value));
        } finally {
            lock.unlock();
        }
    }

    public String param(Param key) {
        return params.get(key);
    }

    public void param(Param key, String value) {
        params.put(key, value);
    }

    public String requiredParam(Param key) {
        return Asserts.notNull(params.get(key), "param is required, param={}", key.key);
    }

    public void printOutputs() {
        lock.lock();
        try {
            if (newOutputs.isEmpty()) {
                messageLogger.info("\nno outputs\n");
                return;
            }
            messageLogger.info("\noutputs:\n");
            newOutputs.forEach((key, values) -> values.forEach(value -> messageLogger.info("{} => {}\n", key, value)));
        } finally {
            lock.unlock();
        }
    }
}
