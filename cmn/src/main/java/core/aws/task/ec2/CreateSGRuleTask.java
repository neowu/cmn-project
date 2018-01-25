package core.aws.task.ec2;

import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.UserIdGroupPair;
import core.aws.client.AWS;
import core.aws.env.Context;
import core.aws.resource.ec2.Protocol;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.util.ToStringHelper;
import core.aws.workflow.Action;
import core.aws.workflow.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author neo
 */
@Action("create-sg-rule")
public class CreateSGRuleTask extends Task<SecurityGroup> {
    public final Map<Protocol, List<SecurityGroup.Source>> addedIngressRules;

    public CreateSGRuleTask(SecurityGroup securityGroup, Map<Protocol, List<SecurityGroup.Source>> addedIngressRules) {
        super(securityGroup);
        this.addedIngressRules = addedIngressRules;
    }

    @Override
    public void execute(Context context) throws Exception {
        List<IpPermission> permissions = new ArrayList<>();

        addedIngressRules.forEach((protocol, sources) -> {
            IpPermission permission = new IpPermission()
                .withIpProtocol(protocol.ipProtocol)
                .withFromPort(protocol.fromPort)
                .withToPort(protocol.toPort);

            for (SecurityGroup.Source source : sources) {
                if (source.securityGroup != null) {
                    permission.getUserIdGroupPairs()
                        .add(new UserIdGroupPair().withGroupId(source.securityGroup.remoteSecurityGroup.getGroupId()));
                } else if (source.ipRange != null) {
                    permission.getIpv4Ranges().add(source.ipRange);
                }
            }

            permissions.add(permission);
        });

        AWS.getEc2().createSGIngressRules(resource.remoteSecurityGroup.getGroupId(), permissions);
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resource)
            .add("rules", addedIngressRules)
            .toString();
    }
}
