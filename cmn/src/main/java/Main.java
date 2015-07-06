import core.aws.env.Cloud;
import core.aws.env.CloudLoader;
import core.aws.env.Context;
import core.aws.env.Goal;
import core.aws.env.Param;
import core.aws.util.Asserts;
import core.aws.util.ClasspathResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author neo
 */
public class Main {
    public static void main(String[] args) throws Throwable {
        Main main = new Main(args);
        main.execute();
    }

    private final Logger messageLogger = LoggerFactory.getLogger("message");
    private final String[] args;

    public Main(String[] args) {
        this.args = args;
    }

    private void execute() throws Throwable {
        if (args == null || args.length < 1) {
            printHelp();
            return;
        }

        Context context = new Context();
        context.goal = Goal.parse(args[0]);
        parseParams(context);

        Cloud cloud = new CloudLoader().load(context);
        cloud.execute(context);
    }

    private void parseParams(Context context) {
        for (int i = 1, length = args.length; i < length; i++) {
            String arg = args[i];
            Asserts.isTrue(arg.startsWith("--") && arg.contains("="), "arg must be in --{name}={value} format, arg={}", arg);
            int index = arg.indexOf('=');
            Param key = Param.parse(arg.substring(2, index));
            String value = arg.substring(index + 1);
            context.param(key, value);
        }
    }

    private void printHelp() throws IOException {
        messageLogger.info(ClasspathResources.text("help.txt"));
    }
}