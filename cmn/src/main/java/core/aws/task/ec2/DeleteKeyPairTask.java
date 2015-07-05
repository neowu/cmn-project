package core.aws.task.ec2;

import core.aws.env.Context;
import core.aws.resource.ec2.KeyPair;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("del-key")
public class DeleteKeyPairTask extends Task<KeyPair> {
    public DeleteKeyPairTask(KeyPair keyPair) {
        super(keyPair);
    }

    @Override
    public void execute(Context context) throws Exception {
        KeyPairHelper helper = new KeyPairHelper(context.env);
        helper.deleteKeyPair(resource.remoteKeyPair.getKeyName());
    }
}
