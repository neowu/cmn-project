package core.aws.remote.elb.v2;

import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.elb.v2.TargetGroup;

import java.util.List;

public class TargetGroupLoader {
    private final Resources resources;
    private final Environment env;

    public TargetGroupLoader(Resources resources, Environment env) {
        this.resources = resources;
        this.env = env;
    }

    public void load() {
        List<com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup> remoteTGs = AWS.getElbV2().listTGs();
        for (com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup remoteTG : remoteTGs) {
            String tgName = remoteTG.getTargetGroupName();
            String prefix = env.name + "-";
            if (tgName.startsWith(prefix)) {
                String resourceId = tgName.substring(prefix.length());
                TargetGroup tg = resources.find(TargetGroup.class, resourceId).orElseGet(() -> resources.add(new TargetGroup(resourceId)));
                tg.name = tgName;
                tg.remoteTG = remoteTG;
                tg.protocol = remoteTG.getProtocol();
                tg.port = remoteTG.getPort();
                tg.foundInRemote();
            }
        }
    }
}
