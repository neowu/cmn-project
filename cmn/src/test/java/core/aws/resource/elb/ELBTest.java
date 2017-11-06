package core.aws.resource.elb;

import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.ListenerDescription;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ELBTest {
    ELB elb;

    @BeforeEach
    void createELB() {
        elb = new ELB("elb");
    }

    @Test
    void httpsCertChangedWithNewLocalCert() {
        elb.listenHTTPS = true;
        elb.remoteELB = new LoadBalancerDescription()
                .withListenerDescriptions(new ListenerDescription().withListener(new Listener("HTTPS", 443, 80)));
        elb.cert = new ServerCert("cert");
        elb.cert.foundInLocal();

        assertTrue(elb.httpsCertChanged());
    }
}
