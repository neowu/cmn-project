package core.aws.task.ec2;

import core.aws.env.Context;
import core.aws.resource.ec2.Instance;
import core.aws.task.linux.LinuxUploader;
import core.aws.workflow.Action;
import core.aws.workflow.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author neo
 */
@Action("upload-instance")
public class UploadTask extends Task<Instance> {
    private final Logger logger = LoggerFactory.getLogger(UploadTask.class);

    public UploadTask(Instance instance) {
        super(instance);
    }

    @Override
    public void execute(Context context) throws Exception {
        logger.info("upload package dir, instanceId={}", resource.id);

        LinuxUploader uploader = new LinuxUploader(context.env, resource.remoteInstances, context);
        uploader.upload();
    }
}
