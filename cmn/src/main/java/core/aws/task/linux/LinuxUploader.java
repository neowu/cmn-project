package core.aws.task.linux;

import com.amazonaws.services.ec2.model.Instance;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;
import core.aws.env.Context;
import core.aws.env.Environment;
import core.aws.env.Param;
import core.aws.resource.ec2.InstanceState;
import core.aws.resource.ec2.KeyPair;
import core.aws.util.SSH;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * @author neo
 */
public class LinuxUploader {
    private final Environment env;
    private final List<Instance> remoteInstances;
    private final Context context;

    public LinuxUploader(Environment env, List<Instance> remoteInstances, Context context) {
        this.env = env;
        this.remoteInstances = remoteInstances;
        this.context = context;
    }

    public void upload() throws IOException, JSchException, SftpException, InterruptedException {
        String packageDir = context.requiredParam(Param.PACKAGE_DIR);

        String index = context.param(Param.INSTANCE_INDEX);

        for (int i = 0; i < remoteInstances.size(); i++) {
            Instance remoteInstance = remoteInstances.get(i);
            if (InstanceState.RUNNING.equalsTo(remoteInstance.getState()) && indexMatches(index, i)) {
                try (SSH ssh = new SSH(remoteInstance.getPublicDnsName(), "ubuntu", KeyPair.keyFile(remoteInstance.getKeyName(), env))) {
                    ssh.uploadDir(Paths.get(packageDir), "/opt/packages");
                }
            }
        }
    }

    private boolean indexMatches(String requestedIndex, int instanceInstance) {
        return requestedIndex == null || Integer.parseInt(requestedIndex) == instanceInstance;
    }
}
