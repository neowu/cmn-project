package core.aws.remote.ec2;

import com.amazonaws.services.ec2.model.DescribeKeyPairsRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.KeyPairInfo;
import core.aws.client.AWS;
import core.aws.env.Environment;
import core.aws.resource.Resources;
import core.aws.resource.ec2.KeyPair;

import java.util.List;
import java.util.Optional;

/**
 * @author neo
 */
public class KeyPairLoader {
    private final Resources resources;
    private final Environment env;

    public KeyPairLoader(Resources resources, Environment env) {
        this.resources = resources;
        this.env = env;
    }

    public void load() {
        List<KeyPairInfo> remoteKeyPairs = AWS.ec2.ec2.describeKeyPairs(new DescribeKeyPairsRequest()
            .withFilters(new Filter("key-name").withValues(env.name + ":*"))).getKeyPairs();

        for (KeyPairInfo remoteKeyPair : remoteKeyPairs) {
            String keyPairId = keyPairId(env.name, remoteKeyPair.getKeyName());
            if (keyPairId != null) {
                Optional<KeyPair> result = resources.find(KeyPair.class, keyPairId);
                KeyPair keyPair = result.isPresent() ? result.get() : resources.add(new KeyPair(keyPairId, remoteKeyPair.getKeyName()));
                keyPair.remoteKeyPair = remoteKeyPair;
                keyPair.foundInRemote();
            }
        }
    }

    String keyPairId(String envName, String keyPairName) {
        if (keyPairName.length() <= envName.length() + 1) return null;  // ${env.name}:${name}
        return keyPairName.substring(envName.length() + 1);
    }
}
