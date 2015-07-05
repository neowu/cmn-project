package core.aws.resource.elb;

import com.amazonaws.services.elasticloadbalancing.model.Listener;
import com.amazonaws.services.elasticloadbalancing.model.ListenerDescription;
import com.amazonaws.services.elasticloadbalancing.model.LoadBalancerDescription;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ELBTest {
    ELB elb;

    @Before
    public void createELB() {
        elb = new ELB("elb");
    }

    @Test
    public void httpsCertChangedWithNewLocalCert() {
        elb.listenHTTPS = true;
        elb.remoteELB = new LoadBalancerDescription()
            .withListenerDescriptions(new ListenerDescription().withListener(new Listener("HTTPS", 443, 80)));
        elb.cert = new ServerCert("cert");
        elb.cert.foundInLocal();

        Assert.assertTrue(elb.httpsCertChanged());
    }
}