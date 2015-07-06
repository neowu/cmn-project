package core.aws.remote.ec2;

import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.ec2.InstanceProfile;

import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
public class InstanceProfileLoader {
    private final Resources resources;
    private final Environment env;

    public InstanceProfileLoader(Resources resources, Environment env) {
        this.resources = resources;
        this.env = env;
    }

    public void load() {
        List<com.amazonaws.services.identitymanagement.model.InstanceProfile> instanceProfiles = AWS.iam.listInstanceProfiles(InstanceProfile.instanceProfilePath(env));

        String prefix = env.name + "-";
        for (com.amazonaws.services.identitymanagement.model.InstanceProfile remoteInstanceProfile : instanceProfiles) {
            String name = remoteInstanceProfile.getInstanceProfileName();
            if (!name.startsWith(prefix)) continue; // ignore instance profiles not matching naming convention
            String resourceId = name.substring(env.name.length() + 1);

            Optional<InstanceProfile> result = resources.find(InstanceProfile.class, resourceId);
            InstanceProfile instanceProfile = result.isPresent() ? result.get() : resources.add(new InstanceProfile(resourceId));
            instanceProfile.name = remoteInstanceProfile.getInstanceProfileName();
            instanceProfile.remoteInstanceProfile = remoteInstanceProfile;
            instanceProfile.foundInRemote();
        }
    }
}
