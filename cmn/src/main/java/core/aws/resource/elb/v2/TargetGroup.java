package core.aws.resource.elb.v2;

import com.amazonaws.services.elasticloadbalancingv2.model.ProtocolEnum;
import core.aws.resource.Resource;
import core.aws.resource.Resources;
import core.aws.resource.vpc.VPC;
import core.aws.task.elb.v2.CreateTargetGroupTask;
import core.aws.task.elb.v2.DeleteTargetGroupTask;
import core.aws.task.elb.v2.DescribeTargetGroupTask;
import core.aws.task.elb.v2.UpdateTargetGroupTask;
import core.aws.util.Asserts;
import core.aws.workflow.Tasks;

public class TargetGroup extends Resource {
    public com.amazonaws.services.elasticloadbalancingv2.model.TargetGroup remoteTG;
    public String name;
    public String healthCheckURL;
    public VPC vpc;
    public String protocol;
    public Integer port;

    public TargetGroup(String id) {
        super(id);
    }

    @Override
    public void validate(Resources resources) {
        if (protocol != null) {
            Asserts.isTrue("https".equalsIgnoreCase(protocol) || "http".equalsIgnoreCase(protocol), "invalid listener protocol, protocol=" + protocol);
        }
    }

    @Override
    protected void describeTasks(Tasks tasks) {
        tasks.add(new DescribeTargetGroupTask(this));
    }

    @Override
    protected void createTasks(Tasks tasks) {
        tasks.add(new CreateTargetGroupTask(this));
    }

    @Override
    protected void updateTasks(Tasks tasks) {
        if (changed()) {
            tasks.add(new UpdateTargetGroupTask(this));
        }
    }

    private Boolean changed() {
        if (protocol != null && !protocol.equalsIgnoreCase(remoteTG.getProtocol())) {
            return Boolean.TRUE;
        }
        if (port != null && !port.equals(remoteTG.getPort())) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }

    @Override
    protected void deleteTasks(Tasks tasks) {
        tasks.add(new DeleteTargetGroupTask(this));
    }

    public ProtocolEnum protocol() {
        if ("https".equalsIgnoreCase(protocol)) {
            return ProtocolEnum.HTTPS;
        }
        return ProtocolEnum.HTTP;
    }
}
