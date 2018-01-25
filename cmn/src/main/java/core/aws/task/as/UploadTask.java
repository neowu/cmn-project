package core.aws.task.as;

import com.amazonaws.services.autoscaling.model.Instance;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.as.ASGroup;
import core.aws.task.linux.LinuxUploader;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This task is only for troubleshooting purpose, since AS group, any permanent changes need to be baked in AMI
 *
 * @author neo
 */
@Action("upload-asg")
public class UploadTask extends Task<ASGroup> {
    private final Logger logger = LoggerFactory.getLogger(UploadTask.class);

    public UploadTask(ASGroup asGroup) {
        super(asGroup);
    }

    @Override
    public void execute(Context context) throws Exception {
        logger.info("upload package dir, asGroupId={}", resource.id);

        List<String> instanceIds = resource.remoteASGroup.getInstances().stream()
            .map(Instance::getInstanceId)
            .collect(Collectors.toList());

        List<com.amazonaws.services.ec2.model.Instance> remoteInstances = AWS.getEc2().describeInstances(instanceIds);

        LinuxUploader uploader = new LinuxUploader(context.env, remoteInstances, context);
        uploader.upload();
    }
}
