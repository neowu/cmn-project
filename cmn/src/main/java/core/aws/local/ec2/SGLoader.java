package core.aws.local.ec2;

import com.amazonaws.services.ec2.model.IpRange;
import core.aws.env.Environment;
import core.aws.local.DependencyResolvers;
import core.aws.local.LocalResourceLoader;
import core.aws.local.ResourceNode;
import core.aws.resource.Resources;
import core.aws.resource.ec2.Protocol;
import core.aws.resource.ec2.SecurityGroup;
import core.aws.util.Asserts;
import core.aws.util.Randoms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author neo
 */
public class SGLoader implements LocalResourceLoader {
    @Override
    @SuppressWarnings("unchecked")
    public void load(ResourceNode node, final Resources resources, DependencyResolvers resolvers, Environment env) {
        Map<Protocol, List<Source>> ingressRules = new HashMap<>();

        List<Map<String, Object>> rules = (List<Map<String, Object>>) node.listField("ingress");
        for (Map<String, Object> rule : rules) {
            String cidr = (String) rule.get("cidr");
            String sgId = (String) rule.get("security-group");
            Object protocolParam = Asserts.notNull(rule.get("protocol"), "protocol is required for ingress");

            List<Object> protocols;

            if (protocolParam instanceof List) {
                protocols = (List<Object>) protocolParam;
            } else {
                protocols = new ArrayList<>();
                protocols.add(protocolParam);
            }

            for (Object value : protocols) {
                Protocol protocol = Protocol.parse(String.valueOf(value));
                Source source = new Source();
                ingressRules.computeIfAbsent(protocol, key -> new ArrayList<>()).add(source);
                if (cidr != null) {
                    source.cidr = cidr;
                } else if (sgId != null) {
                    source.sgId = sgId;
                } else {
                    throw new Error("ingress requires cidr or security-group");
                }
            }
        }

        SecurityGroup securityGroup = resources.add(new SecurityGroup(node.id));
        securityGroup.name = env.name + ":" + node.id + ":" + Randoms.alphaNumeric(6);
        securityGroup.vpc = resources.vpc;

        resolvers.add(node, () -> {
            ingressRules.forEach((protocol, sources) ->
                sources.forEach(source -> addIngressRule(securityGroup, protocol, source, resources)));
        });
    }

    private void addIngressRule(SecurityGroup securityGroup, Protocol protocol, Source inputSource, Resources resources) {
        SecurityGroup.Source source = new SecurityGroup.Source();
        if (inputSource.sgId != null) {
            source.securityGroup = resources.get(SecurityGroup.class, inputSource.sgId);
        } else if (inputSource.cidr != null) {
            source.ipRange = new IpRange().withCidrIp(inputSource.cidr);
        }
        securityGroup.addIngressRule(protocol, source);
    }

    public static class Source {
        String sgId;
        String cidr;
    }
}
