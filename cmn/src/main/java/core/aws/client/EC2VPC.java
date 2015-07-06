package core.aws.client;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.regions.Region;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Address;
import com.amazonaws.services.ec2.model.AllocateAddressRequest;
import com.amazonaws.services.ec2.model.AllocateAddressResult;
import com.amazonaws.services.ec2.model.AssociateAddressRequest;
import com.amazonaws.services.ec2.model.AttachInternetGatewayRequest;
import com.amazonaws.services.ec2.model.CreateSubnetRequest;
import com.amazonaws.services.ec2.model.CreateVpcRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesRequest;
import com.amazonaws.services.ec2.model.DescribeAddressesResult;
import com.amazonaws.services.ec2.model.DescribeRouteTablesRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsRequest;
import com.amazonaws.services.ec2.model.DescribeVpcsResult;
import com.amazonaws.services.ec2.model.DisassociateAddressRequest;
import com.amazonaws.services.ec2.model.Filter;
import com.amazonaws.services.ec2.model.InternetGateway;
import com.amazonaws.services.ec2.model.ModifyVpcAttributeRequest;
import com.amazonaws.services.ec2.model.ReleaseAddressRequest;
import com.amazonaws.services.ec2.model.RouteTable;
import com.amazonaws.services.ec2.model.Vpc;
import core.aws.util.Runner;
import core.aws.util.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Collection;
import java.util.List;

/**
 * @author neo
 */
public class EC2VPC {
    public final AmazonEC2 ec2;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EC2VPC(AWSCredentialsProvider credentials, Region region) {
        ec2 = new AmazonEC2Client(credentials);
        ec2.setRegion(region);
    }

    public Vpc createVPC() throws InterruptedException {
        logger.info("create VPC");
        String vpcId = ec2.createVpc(new CreateVpcRequest().withCidrBlock("10.0.0.0/16")).getVpc().getVpcId();

        while (true) {
            Threads.sleepRoughly(Duration.ofSeconds(20));
            DescribeVpcsResult result = ec2.describeVpcs(new DescribeVpcsRequest().withVpcIds(vpcId));
            Vpc remoteVPC = result.getVpcs().get(0);
            if ("available".equals(remoteVPC.getState())) {
                enableVPCDNS(vpcId);
                return remoteVPC;
            }
        }
    }

    public com.amazonaws.services.ec2.model.Subnet createSubnet(CreateSubnetRequest request) {
        logger.info("create subnet, request={}", request);
        return ec2.createSubnet(request).getSubnet();
    }

    public List<com.amazonaws.services.ec2.model.Subnet> describeSubnets(List<String> subnetIds) {
        return ec2.describeSubnets(new DescribeSubnetsRequest().withSubnetIds(subnetIds)).getSubnets();
    }

    public Vpc describeVPC(String vpcId) {
        logger.info("describe vpc, vpcId={}", vpcId);
        DescribeVpcsResult result = ec2.describeVpcs(new DescribeVpcsRequest().withVpcIds(vpcId));
        return result.getVpcs().get(0);
    }

    public String createInternetGateway(String vpcId) {
        logger.info("create internet gateway, vpcId={}", vpcId);
        InternetGateway internetGateway = ec2.createInternetGateway().getInternetGateway();
        String internetGatewayId = internetGateway.getInternetGatewayId();
        ec2.attachInternetGateway(new AttachInternetGatewayRequest().withVpcId(vpcId).withInternetGatewayId(internetGatewayId));
        return internetGatewayId;
    }

    private void enableVPCDNS(String vpcId) {
        logger.info("enable VPC DNS support, vpcId={}", vpcId);
        ec2.modifyVpcAttribute(new ModifyVpcAttributeRequest().withVpcId(vpcId)
            .withEnableDnsHostnames(true));
    }

    public String assignEIP(final String instanceId) throws Exception {
        final AllocateAddressResult address = ec2.allocateAddress(new AllocateAddressRequest().withDomain("vpc"));
        logger.info("associate eip to instance, instanceId={}, ip={}", instanceId, address.getPublicIp());

        new Runner<>()
            .retryInterval(Duration.ofSeconds(10))
            .maxAttempts(3)
            .retryOn(e -> e instanceof AmazonServiceException)
            .run(() -> {
                ec2.associateAddress(new AssociateAddressRequest().withInstanceId(instanceId).withAllocationId(address.getAllocationId()));
                return null;
            });

        return address.getPublicIp();
    }

    public void releaseEIP(List<String> instanceIds) {
        logger.info("release EIP for instances, instanceIds={}", instanceIds);

        DescribeAddressesResult result = ec2.describeAddresses(new DescribeAddressesRequest().withFilters(new Filter("instance-id").withValues(instanceIds)));
        for (Address address : result.getAddresses()) {
            logger.info("release EIP, ip={}, instanceId={}", address.getPublicIp(), address.getInstanceId());
            ec2.disassociateAddress(new DisassociateAddressRequest().withAssociationId(address.getAssociationId()));
            ec2.releaseAddress(new ReleaseAddressRequest().withAllocationId(address.getAllocationId()));
        }
    }

    public List<RouteTable> describeRouteTables(Collection<String> routeTableIds) {
        logger.info("describe route tables, routeTableIds={}", routeTableIds);
        return ec2.describeRouteTables(new DescribeRouteTablesRequest().withRouteTableIds(routeTableIds)).getRouteTables();
    }
}