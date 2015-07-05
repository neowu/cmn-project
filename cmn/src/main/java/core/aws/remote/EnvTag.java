package core.aws.remote;

import com.amazonaws.services.ec2.model.TagDescription;
import core.aws.resource.Resource;
import core.aws.resource.ec2.Instance;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.resource.image.Image;
import core.aws.resource.vpc.InternetGateway;
import core.aws.resource.vpc.RouteTable;
import core.aws.resource.vpc.Subnet;
import core.aws.resource.vpc.VPC;
import core.aws.util.Asserts;
import core.aws.util.ToStringHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * @author neo
 */
public class EnvTag {
    private static final Map<String, Class<? extends Resource>> RESOURCE_TYPES = new HashMap<>();

    static {
        RESOURCE_TYPES.put("instance", Instance.class);
        RESOURCE_TYPES.put("security-group", SecurityGroup.class);
        RESOURCE_TYPES.put("image", Image.class);
        RESOURCE_TYPES.put("vpc", VPC.class);
        RESOURCE_TYPES.put("route-table", RouteTable.class);
        RESOURCE_TYPES.put("internet-gateway", InternetGateway.class);
        RESOURCE_TYPES.put("subnet", Subnet.class);
    }

    public final Class<? extends Resource> resourceClass;
    public final String remoteResourceId;
    private final Map<String, String> fields = new HashMap<>();

    public EnvTag(Class<? extends Resource> resourceClass, String remoteResourceId) {
        this.resourceClass = resourceClass;
        this.remoteResourceId = remoteResourceId;
    }

    public static EnvTag newTag(TagDescription remoteTag) {
        String resourceType = remoteTag.getResourceType();
        Class<? extends Resource> resourceClass = Asserts.notNull(RESOURCE_TYPES.get(resourceType), "not supported resourceType, type={}", resourceType);
        return new EnvTag(resourceClass, remoteTag.getResourceId());
    }

    public void addField(TagDescription remoteTag) {
        String key = remoteTag.getKey();
        int index = key.lastIndexOf(':');
        String field = key.substring(index + 1);
        fields.put(field, remoteTag.getValue());
    }

    public String resourceId() {
        return Asserts.notNull(fields.get("resource-id"), "can not find resource-id, fields={}", fields);
    }

    public String type() {
        return fields.get("type");
    }

    public Integer version() {
        return Integer.parseInt(fields.get("version"));
    }

    @Override
    public String toString() {
        return new ToStringHelper(this)
            .add(resourceClass)
            .add(fields)
            .add("remoteResourceId", remoteResourceId)
            .toString();
    }
}
