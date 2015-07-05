package core.aws.task.ec2;

import core.aws.env.Context;
import core.aws.env.Param;
import core.aws.resource.ec2.Instance;
import core.aws.resource.ec2.InstanceState;
import core.aws.resource.image.Image;
import core.aws.task.linux.AnsibleProvisioner;
import core.aws.util.Asserts;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
@Action("provision-instance")
public class ProvisionInstanceTask extends Task<Instance> {
    private final Logger logger = LoggerFactory.getLogger(ProvisionInstanceTask.class);

    public ProvisionInstanceTask(Instance instance) {
        super(instance);
    }

    @Override
    public void execute(Context context) throws Exception {
        Path playbookPath = playbook(context);
        Optional<Path> packageDir = packageDir(context);

        String index = context.param(Param.INSTANCE_INDEX);
        List<String> provisionedInstanceIds = new ArrayList<>();

        for (int i = 0; i < resource.remoteInstances.size(); i++) {
            com.amazonaws.services.ec2.model.Instance remoteInstance = resource.remoteInstances.get(i);
            logger.info("instance: {} => {}", remoteInstance.getInstanceId(), remoteInstance.getState());

            if (InstanceState.RUNNING.equalsTo(remoteInstance.getState()) && indexMatches(index, i)) {
                provisionedInstanceIds.add(remoteInstance.getInstanceId());
                AnsibleProvisioner provisioner = new AnsibleProvisioner(context.env, remoteInstance, playbookPath, packageDir);
                provisioner.provision();
            }
        }

        context.output(String.format("instance/%s", resource.id), "provisionedInstances=" + provisionedInstanceIds);
    }

    private Path playbook(Context context) {
        String playbook = context.param(Param.PROVISION_PLAYBOOK);
        if (playbook != null) {
            Path playbookPath = context.env.envDir.resolve(playbook);
            Asserts.isTrue(Files.exists(playbookPath), "playbook does not exist, path={}", playbookPath);
            return playbookPath;
        } else if (resource.ami instanceof Image) {
            return ((Image) resource.ami).playbook;
        }
        throw new Error("resource is not using Image, please specify playbook");
    }

    private Optional<Path> packageDir(Context context) {
        String packageDir = context.param(Param.PACKAGE_DIR);
        if (packageDir != null) {
            Path packageDirPath = context.env.envDir.resolve(packageDir);
            Asserts.isTrue(Files.exists(packageDirPath), "packageDir does not exist, path={}", packageDirPath);
            return Optional.of(packageDirPath);
        } else if (resource.ami instanceof Image) {
            return ((Image) resource.ami).packageDir;
        }
        return Optional.empty();
    }

    private boolean indexMatches(String requestedIndex, int instanceInstance) {
        return requestedIndex == null || Integer.parseInt(requestedIndex) == instanceInstance;
    }
}
