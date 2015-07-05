package core.aws.task.ec2;

import core.aws.env.Context;
import core.aws.resource.ec2.KeyPair;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

/**
 * @author neo
 */
@Action("create-key")
public class CreateKeyPairTask extends Task<KeyPair> {
    public CreateKeyPairTask(KeyPair keyPair) {
        super(keyPair);
    }

    @Override
    public void execute(Context context) throws Exception {
        KeyPairHelper helper = new KeyPairHelper(context.env);
        helper.createKeyPair(resource);
    }
}
