package core.aws.env;

import core.aws.util.Asserts;
import core.aws.util.Lists;
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
    final Map<Param, List<String>> params = new ConcurrentHashMap<>();
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

    public List<String> params(Param key) {
        return params.get(key);
    }

    public String param(Param key) {
        List<String> params = this.params.get(key);
        if (params == null) return null;
        if (params.size() == 1) return params.get(0);
        throw new IllegalStateException("found multiple values, key=" + key);
    }

    public void param(Param key, String value) {
        params.computeIfAbsent(key, k -> Lists.newArrayList()).add(value);
    }

    public String requiredParam(Param key) {
        return Asserts.notNull(param(key), "param is required, param={}", key.key);
    }

    void printOutputs() {
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
