package core.aws.remote.ec2;

import com.amazonaws.services.ec2.model.DescribeSecurityGroupsRequest;
import core.aws.client.AWS;
import core.aws.remote.EnvTag;
import core.aws.remote.Loader;
import core.aws.resource.Resources;
import core.aws.resource.ec2.SecurityGroup;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author neo
 */
public class SGLoader extends Loader {
    public SGLoader(Resources resources, List<EnvTag> tags) {
        super(resources, tags);
    }

    @Override
    public void load() {
        Map<String, SecurityGroup> remoteSecurityGroups = new HashMap<>();

        all(SecurityGroup.class)
            .forEach(tag -> {
                SecurityGroup securityGroup = resources.find(SecurityGroup.class, tag.resourceId())
                    .orElseGet(() -> resources.add(new SecurityGroup(tag.resourceId())));
                securityGroup.foundInRemote();
                remoteSecurityGroups.put(tag.remoteResourceId, securityGroup);
            });

        if (!remoteSecurityGroups.isEmpty())
            loadSecurityGroups(remoteSecurityGroups);
    }

    private void loadSecurityGroups(Map<String, SecurityGroup> securityGroups) {
        DescribeSecurityGroupsRequest request = new DescribeSecurityGroupsRequest()
            .withGroupIds(securityGroups.keySet());

        for (com.amazonaws.services.ec2.model.SecurityGroup remoteSecurityGroup : AWS.ec2.ec2.describeSecurityGroups(request).getSecurityGroups()) {
            SecurityGroup securityGroup = securityGroups.get(remoteSecurityGroup.getGroupId());
            securityGroup.name = remoteSecurityGroup.getGroupName();
            securityGroup.remoteSecurityGroup = remoteSecurityGroup;
        }
    }
}
