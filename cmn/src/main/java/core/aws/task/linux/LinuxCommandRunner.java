package core.aws.task.linux;

import com.amazonaws.services.ec2.model.Instance;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import core.aws.env.Context;
import core.aws.env.Environment;
import core.aws.env.Param;
import core.aws.resource.ec2.InstanceState;
import core.aws.resource.ec2.KeyPair;
import core.aws.util.Asserts;
import core.aws.util.SSH;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * @author neo
 */
public class LinuxCommandRunner {
    private final Environment env;
    private final List<Instance> remoteInstances;
    private final Context context;

    public LinuxCommandRunner(Environment env, List<Instance> remoteInstances, Context context) {
        this.env = env;
        this.remoteInstances = remoteInstances;
        this.context = context;
    }

    public void run() throws IOException, JSchException, SftpException, InterruptedException {
        List<String> commands = context.params(Param.EXECUTE_COMMAND);
        String script = context.param(Param.EXECUTE_SCRIPT);

        Asserts.isTrue(commands != null || script != null, "{} or {} is required", Param.EXECUTE_COMMAND.key, Param.EXECUTE_SCRIPT.key);

        String index = context.param(Param.INSTANCE_INDEX);

        for (int i = 0; i < remoteInstances.size(); i++) {
            Instance remoteInstance = remoteInstances.get(i);
            if (InstanceState.RUNNING.equalsTo(remoteInstance.getState()) && indexMatches(index, i)) {
                try (SSH ssh = new SSH(remoteInstance.getPublicDnsName(), "ubuntu", KeyPair.keyFile(remoteInstance.getKeyName(), env))) {
                    if (commands != null) {
                        ssh.executeCommands(commands.toArray(new String[0]));
                    } else {
                        executeScript(ssh, env.envDir.resolve(script));
                    }
                }
            }
        }
    }

    private boolean indexMatches(String requestedIndex, int instanceInstance) {
        return requestedIndex == null || Integer.parseInt(requestedIndex) == instanceInstance;
    }

    private void executeScript(SSH ssh, Path scriptPath) throws IOException, SftpException, JSchException, InterruptedException {
        String path = "/tmp/" + scriptPath.getFileName().toString();
        ssh.put(scriptPath, path);
        ssh.executeCommands("chmod +rx " + path, path);
    }
}
