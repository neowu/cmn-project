package core.aws.resource.ec2;

import com.amazonaws.services.ec2.model.KeyPairInfo;
import core.aws.env.Environment;
import core.aws.resource.Resource;
import core.aws.resource.ResourceStatus;
import core.aws.resource.Resources;
import core.aws.task.ec2.CreateKeyPairTask;
import core.aws.task.ec2.DeleteKeyPairTask;
import core.aws.util.Asserts;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Tasks;

import java.nio.file.Path;

/**
 * @author neo
 */
public class KeyPair extends Resource {
    public static Path keyFile(String keyName, Environment env) {
        return env.envDir.resolve("keys/" + normalizeName(keyName) + ".pem");
    }

    static String normalizeName(String keyName) {
        return keyName.replaceAll(":", "-");
    }

    public final String name;
    public KeyPairInfo remoteKeyPair;

    public KeyPair(String id, String name) {
        super(id);
        this.name = name;
    }

    @Override
    public void validate(Resources resources) {
        if (status == ResourceStatus.LOCAL_ONLY) {
            Asserts.isTrue(name.length() <= 255, "max length of key pair name is 255");
            Asserts.isTrue(name.matches("[a-zA-Z0-9\\-\\:]+"), "key pair name can only contain alphanumeric, '-' and ':'");
        }
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateKeyPairTask(this));
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteKeyPairTask(this));
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(id)
            .add(status)
            .add("name", name)
            .toString();
    }
}
