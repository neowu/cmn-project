package core.aws.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * @author neo
 */
public class Runner<T> {
    private final Logger logger = LoggerFactory.getLogger(Runner.class);

    int maxAttempts;
    Duration retryInterval;
    Predicate<Exception> predicate;

    public T run(Callable<T> task) throws Exception {
        int attempts = 0;
        while (true) {
            try {
                attempts++;
                return task.call();
            } catch (Exception e) {
                if (attempts >= maxAttempts || !predicate.test(e)) throw e;
                logger.warn("failed to execute aws operation, retry soon", e);
                Threads.sleepRoughly(retryInterval.multipliedBy((long) (Math.pow(2, attempts - 1))));
            }
        }
    }

    public Runner<T> maxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
        return this;
    }

    public Runner<T> retryInterval(Duration retryInterval) {
        this.retryInterval = retryInterval;
        return this;
    }

    public Runner<T> retryOn(Predicate<Exception> predicate) {
        this.predicate = predicate;
        return this;
    }
}
