package core.aws.resource.ec2;

import core.aws.util.ToStringHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author neo
 */
public class Protocol {
    private static final Pattern SINGLE_PORT_PATTERN = Pattern.compile("\\d+");
    private static final Pattern PORT_RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)");

    static final String TCP = "tcp";
    private static final String UDP = "udp";

    private static final Map<String, Protocol> PREDEFINED_PROTOCOLS = new HashMap<>();

    static {
        PREDEFINED_PROTOCOLS.put("http", new Protocol(TCP, 80));
        PREDEFINED_PROTOCOLS.put("https", new Protocol(TCP, 443));
        PREDEFINED_PROTOCOLS.put("ssh", new Protocol(TCP, 22));
        PREDEFINED_PROTOCOLS.put("memcached", new Protocol(TCP, 11211));
        PREDEFINED_PROTOCOLS.put("mssql", new Protocol(TCP, 1433));
        PREDEFINED_PROTOCOLS.put("mysql", new Protocol(TCP, 3306));
        PREDEFINED_PROTOCOLS.put("rdp", new Protocol(TCP, 3389));
        PREDEFINED_PROTOCOLS.put("mongodb", new Protocol(TCP, 27017));
        PREDEFINED_PROTOCOLS.put("rabbitmq", new Protocol(TCP, 5672));
        PREDEFINED_PROTOCOLS.put("elasticsearch", new Protocol(TCP, 9300));
        PREDEFINED_PROTOCOLS.put("redis", new Protocol(TCP, 6379));
        PREDEFINED_PROTOCOLS.put("openvpn", new Protocol(UDP, 1194));
    }

    public static Protocol parse(String value) {
        Protocol protocol = PREDEFINED_PROTOCOLS.get(value.toLowerCase());
        if (protocol != null) return protocol;

        Matcher matcher = SINGLE_PORT_PATTERN.matcher(value);
        if (matcher.matches()) return new Protocol(TCP, Integer.parseInt(matcher.group()));

        matcher = PORT_RANGE_PATTERN.matcher(value);
        if (matcher.matches())
            return new Protocol(TCP, Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));

        throw new IllegalArgumentException("unknown protocol, protocol=" + value);
    }

    public final String ipProtocol;
    public final int fromPort;
    public final int toPort;

    public Protocol(String ipProtocol, int port) {
        this(ipProtocol, port, port);
    }

    public Protocol(String ipProtocol, int fromPort, int toPort) {
        this.ipProtocol = ipProtocol;
        this.fromPort = fromPort;
        this.toPort = toPort;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;

        Protocol protocol = (Protocol) object;

        return fromPort == protocol.fromPort
            && toPort == protocol.toPort
            && ipProtocol.equals(protocol.ipProtocol);
    }

    @Override
    public int hashCode() {
        int result = ipProtocol.hashCode();
        result = 31 * result + fromPort;
        result = 31 * result + toPort;
        return result;
    }

    @Override
    public String toString() {
        ToStringHelper helper = new ToStringHelper(this);
        if (fromPort == toPort) {
            helper.add(fromPort);
        } else {
            helper.add(fromPort + "-" + toPort);
        }
        return helper.toString();
    }
}
