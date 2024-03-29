import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.models.ProductNative;
import ufu.davigabriel.models.ReplyNative;
import ufu.davigabriel.server.*;
import utils.RandomUtils;

import java.io.IOException;

/*
Para estes, basta executar antes o ./runOnlyReplicas.sh
ou seja, basta que réplicas RATIS estejam de pé
 */
@Ignore
public class AdminPortalServerTest {
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    public static int TOLERANCE_MS = 1000;

    @Test
    public void shouldCrdClient() throws IOException, InterruptedException {
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                                     .forName(serverName).directExecutor().addService(new AdminPortalServer.AdminPortalImpl()).build().start());

        AdminPortalGrpc.AdminPortalBlockingStub blockingStub = AdminPortalGrpc.newBlockingStub(
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

        Client clientThatShouldBeCreated = RandomUtils.generateRandomClient().toClient();
        Client clientThatShouldNotBeCreated = RandomUtils.generateRandomClient().toClient();

        Reply reply = blockingStub.createClient(clientThatShouldBeCreated);
        Client client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldBeCreated.getCID()).build());
        Assert.assertEquals(clientThatShouldBeCreated, client);
        Assert.assertNotEquals(clientThatShouldNotBeCreated, client);
        client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldNotBeCreated.getCID()).build());
        Assert.assertNotEquals(clientThatShouldNotBeCreated, client);

        clientThatShouldBeCreated = clientThatShouldNotBeCreated;
        reply = blockingStub.createClient(clientThatShouldBeCreated);
        Assert.assertNotEquals(clientThatShouldBeCreated, client);
        client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldBeCreated.getCID()).build());
        Assert.assertEquals(clientThatShouldBeCreated, client);

        reply = blockingStub.deleteClient(ID.newBuilder().setID(clientThatShouldBeCreated.getCID()).build());
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldNotBeCreated.getCID()).build());
        Assert.assertNotEquals(clientThatShouldBeCreated, client);
    }

    @Test
    public void shouldUpdateClientOnlyWhenCorrectHash() throws IOException, InterruptedException {
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                                     .forName(serverName).directExecutor().addService(new AdminPortalServer.AdminPortalImpl()).build().start());

        AdminPortalGrpc.AdminPortalBlockingStub blockingStub = AdminPortalGrpc.newBlockingStub(
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

        ClientNative clientNativeThatShouldBeCreated = RandomUtils.generateRandomClient();
        Reply reply = blockingStub.createClient(clientNativeThatShouldBeCreated.toClient());
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        reply = blockingStub.createClient(clientNativeThatShouldBeCreated.toClient());
        Assert.assertEquals(reply.getError(), ReplyNative.DUPLICATA.getError());

        ClientNative clientNativeThatShouldNotBeUpdated = ClientNative.fromClient(clientNativeThatShouldBeCreated.toClient());
        clientNativeThatShouldNotBeUpdated.setZipCode("123");
        clientNativeThatShouldNotBeUpdated.setUpdatedVersionHash("");
        reply = blockingStub.updateClient(clientNativeThatShouldNotBeUpdated.toClient());
        Assert.assertEquals(reply.getError(), ReplyNative.VERSAO_CONFLITANTE.getError());

        ClientNative clientNativeThatShouldBeUpdated = ClientNative.fromClient(clientNativeThatShouldBeCreated.toClient());
        clientNativeThatShouldBeUpdated.setZipCode("123");
        clientNativeThatShouldBeUpdated.setUpdatedVersionHash(clientNativeThatShouldBeCreated.getHash());
        reply = blockingStub.updateClient(clientNativeThatShouldBeUpdated.toClient());
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
    }

    @Test
    public void shouldCrdProductOneServer() throws IOException, InterruptedException {
        Product productThatShouldBeCreated = RandomUtils.generateRandomProduct().toProduct();
        Product productThatShouldNotBeCreated = RandomUtils.generateRandomProduct().toProduct();

        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new AdminPortalServer.AdminPortalImpl()).build().start());

        AdminPortalGrpc.AdminPortalBlockingStub blockingStub = AdminPortalGrpc.newBlockingStub(
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

        Reply reply = blockingStub.createProduct(productThatShouldBeCreated);
        Thread.sleep(TOLERANCE_MS);
        Product product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(productThatShouldBeCreated, product);
        Assert.assertNotEquals(productThatShouldNotBeCreated, product);
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldNotBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertNotEquals(productThatShouldNotBeCreated, product);
        productThatShouldBeCreated = productThatShouldNotBeCreated;
        reply = blockingStub.createProduct(productThatShouldBeCreated);
        Thread.sleep(TOLERANCE_MS);
        Assert.assertNotEquals(productThatShouldBeCreated, product);
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(productThatShouldBeCreated, product);

        reply = blockingStub.deleteProduct(ID.newBuilder().setID(productThatShouldBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertNotEquals(productThatShouldBeCreated, product);
    }

    @Test
    public void shouldUpdateProductOneServer() throws IOException, InterruptedException {
        Product productThatShouldBeCreated = RandomUtils.generateRandomProduct().toProduct();
        Product productThatShouldNotBeCreated = RandomUtils.generateRandomProduct().toProduct();

        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                                     .forName(serverName).directExecutor().addService(new AdminPortalServer.AdminPortalImpl()).build().start());

        AdminPortalGrpc.AdminPortalBlockingStub blockingStub = AdminPortalGrpc.newBlockingStub(
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

        Reply reply = blockingStub.createProduct(productThatShouldBeCreated);
        Thread.sleep(TOLERANCE_MS);
        Product product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(productThatShouldBeCreated, product);
        Assert.assertNotEquals(productThatShouldNotBeCreated, product);
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldNotBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertNotEquals(productThatShouldNotBeCreated, product);
        productThatShouldBeCreated = productThatShouldNotBeCreated;
        reply = blockingStub.createProduct(productThatShouldBeCreated);
        Thread.sleep(TOLERANCE_MS);
        Assert.assertNotEquals(productThatShouldBeCreated, product);
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(productThatShouldBeCreated, product);

        ProductNative productThatShouldNotBeUpdated = ProductNative.fromProduct(productThatShouldBeCreated);
        productThatShouldNotBeUpdated.setDescription("dkjshabuipokejxm");
        reply = blockingStub.updateProduct(productThatShouldNotBeUpdated.toProduct());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(reply.getError(), ReplyNative.VERSAO_CONFLITANTE.getError());
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldNotBeUpdated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertNotEquals(productThatShouldNotBeUpdated.toProduct(), product);

        ProductNative productThatShouldActuallyBeUpdated = ProductNative.fromProduct(productThatShouldBeCreated);
        productThatShouldActuallyBeUpdated.setDescription("dkjshabuipokejxm");
        productThatShouldActuallyBeUpdated.setUpdatedVersionHash(ProductNative.fromProduct(productThatShouldBeCreated).getHash());
        reply = blockingStub.updateProduct(productThatShouldActuallyBeUpdated.toProduct());
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldActuallyBeUpdated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(productThatShouldActuallyBeUpdated.toProduct(), product);
    }

    @Test
    public void shouldCrdClientOnDelay() throws IOException, InterruptedException {
        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                                     .forName(serverName).directExecutor().addService(new AdminPortalServer.AdminPortalImpl()).build().start());

        AdminPortalGrpc.AdminPortalBlockingStub blockingStub = AdminPortalGrpc.newBlockingStub(
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

        Client clientThatShouldBeCreated = RandomUtils.generateRandomClient().toClient();
        Client clientThatShouldNotBeCreated = RandomUtils.generateRandomClient().toClient();

        Reply reply = blockingStub.createClient(clientThatShouldBeCreated);
        Client client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldBeCreated.getCID()).build());
        Assert.assertEquals(clientThatShouldBeCreated, client);
        Thread.sleep(31*1000);
        client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldBeCreated.getCID()).build());
        Assert.assertEquals(clientThatShouldBeCreated, client);
        Assert.assertNotEquals(clientThatShouldNotBeCreated, client);
        client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldNotBeCreated.getCID()).build());
        Assert.assertNotEquals(clientThatShouldNotBeCreated, client);

        clientThatShouldBeCreated = clientThatShouldNotBeCreated;
        reply = blockingStub.createClient(clientThatShouldBeCreated);
        Assert.assertNotEquals(clientThatShouldBeCreated, client);
        client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldBeCreated.getCID()).build());
        Assert.assertEquals(clientThatShouldBeCreated, client);

        reply = blockingStub.deleteClient(ID.newBuilder().setID(clientThatShouldBeCreated.getCID()).build());
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(31*1000);
        client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldNotBeCreated.getCID()).build());
        Assert.assertNotEquals(clientThatShouldBeCreated, client);
    }

    @Test
    public void shouldCrdProductOneServerOnDelay() throws IOException, InterruptedException {
        Product productThatShouldBeCreated = RandomUtils.generateRandomProduct().toProduct();
        Product productThatShouldNotBeCreated = RandomUtils.generateRandomProduct().toProduct();

        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                                     .forName(serverName).directExecutor().addService(new AdminPortalServer.AdminPortalImpl()).build().start());

        AdminPortalGrpc.AdminPortalBlockingStub blockingStub = AdminPortalGrpc.newBlockingStub(
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

        Reply reply = blockingStub.createProduct(productThatShouldBeCreated);
        Thread.sleep(TOLERANCE_MS);
        Product product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(productThatShouldBeCreated, product);
        Thread.sleep(31*1000);
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldBeCreated.getPID()).build());
        Assert.assertEquals(productThatShouldBeCreated, product);
        Assert.assertNotEquals(productThatShouldNotBeCreated, product);
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldNotBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertNotEquals(productThatShouldNotBeCreated, product);
        productThatShouldBeCreated = productThatShouldNotBeCreated;
        reply = blockingStub.createProduct(productThatShouldBeCreated);
        Thread.sleep(TOLERANCE_MS);
        Assert.assertNotEquals(productThatShouldBeCreated, product);
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(productThatShouldBeCreated, product);

        reply = blockingStub.deleteProduct(ID.newBuilder().setID(productThatShouldBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productThatShouldBeCreated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertNotEquals(productThatShouldBeCreated, product);
    }
}
