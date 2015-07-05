package core.aws.resource.ec2;

import com.amazonaws.services.ec2.model.IpPermission;
import com.amazonaws.services.ec2.model.UserIdGroupPair;
import core.aws.util.Lists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author neo
 */
class SecurityGroupRuleHelper {
    private final Map<Protocol, List<SecurityGroup.Source>> localIngressRules;
    private final List<IpPermission> remoteIngressRules;

    SecurityGroupRuleHelper(Map<Protocol, List<SecurityGroup.Source>> localIngressRules, List<IpPermission> remoteIngressRules) {
        this.localIngressRules = localIngressRules;
        this.remoteIngressRules = remoteIngressRules;
    }

    Map<Protocol, List<SecurityGroup.Source>> findAddedIngressRules() {
        Map<Protocol, List<SecurityGroup.Source>> addedRules = new HashMap<>();

        localIngressRules.forEach((protocol, sources) -> sources.forEach(source -> {
            if (!remoteIngressRulesContains(protocol, source)) {
                addedRules.computeIfAbsent(protocol, key -> Lists.newArrayList()).add(source);
            }
        }));

        return addedRules;
    }

    List<IpPermission> findDeletedIngressRules() {
        List<IpPermission> deletedRules = new ArrayList<>();

        for (IpPermission permission : remoteIngressRules) {
            // delete all traffic rules
            if ("-1".equals(permission.getIpProtocol())) {
                deletedRules.add(permission);
                continue;
            }

            Protocol protocol = new Protocol(permission.getIpProtocol(), permission.getFromPort(), permission.getToPort());
            List<SecurityGroup.Source> sources = localIngressRules.get(protocol);
            if (sources == null) {
                deletedRules.add(permission);
                continue;
            }

            List<String> deletedIpRanges = permission.getIpRanges().stream()
                .filter(ipRange -> !containsIpRange(sources, ipRange))
                .collect(Collectors.toList());

            List<UserIdGroupPair> deletedSecurityGroupIds = new ArrayList<>();
            deletedSecurityGroupIds.addAll(permission.getUserIdGroupPairs().stream()
                .filter(userGroup -> !containsSourceUserGroup(sources, userGroup))
                .map(userGroup -> new UserIdGroupPair().withUserId(userGroup.getUserId()).withGroupId(userGroup.getGroupId()))
                .collect(Collectors.toList()));

            if (!deletedIpRanges.isEmpty() || !deletedSecurityGroupIds.isEmpty()) {
                IpPermission rule = new IpPermission()
                    .withIpProtocol(permission.getIpProtocol())
                    .withFromPort(permission.getFromPort())
                    .withToPort(permission.getToPort())
                    .withIpRanges(deletedIpRanges)
                    .withUserIdGroupPairs(deletedSecurityGroupIds);
                deletedRules.add(rule);
            }
        }

        return deletedRules;
    }

    private boolean containsSourceUserGroup(Collection<SecurityGroup.Source> sources, final UserIdGroupPair sourceUserGroup) {
        return sources.stream().anyMatch(source -> source.securityGroup != null && source.securityGroup.remoteSecurityGroup != null
            && sourceUserGroup.getGroupId().equals(source.securityGroup.remoteSecurityGroup.getGroupId()));
    }

    private boolean containsIpRange(Collection<SecurityGroup.Source> sources, final String ipRange) {
        return sources.stream().anyMatch(source -> source.ipRange != null && source.ipRange.equals(ipRange));
    }

    private boolean remoteIngressRulesContains(Protocol protocol, SecurityGroup.Source source) {
        for (IpPermission rule : remoteIngressRules) {
            if (rule.getIpProtocol().equals(protocol.ipProtocol)
                && rule.getFromPort() == protocol.fromPort
                && rule.getToPort() == protocol.toPort) {
                if (source.ipRange != null && rule.getIpRanges().contains(source.ipRange)) return true;
                if (remoteIngressRuleContainsSource(rule, source)) return true;
            }
        }
        return false;
    }

    private boolean remoteIngressRuleContainsSource(IpPermission rule, SecurityGroup.Source source) {
        if (source.securityGroup != null && source.securityGroup.remoteSecurityGroup != null) {
            for (UserIdGroupPair userGroup : rule.getUserIdGroupPairs()) {
                if (userGroup.getGroupId().equals(source.securityGroup.remoteSecurityGroup.getGroupId()))
                    return true;
            }
        }
        return false;
    }
}


