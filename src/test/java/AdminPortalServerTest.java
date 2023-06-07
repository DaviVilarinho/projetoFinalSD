import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.models.ProductNative;
import ufu.davigabriel.models.ReplyNative;
import ufu.davigabriel.server.*;
import utils.RandomUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

/*
Para executar estes testes, basta executar o Mosquitto em um terminal.
 */
public class AdminPortalServerTest {
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    public static int TOLERANCE_MS = 1000;

    @Test
    public void shouldCrudClientOneServer() throws IOException, InterruptedException {
        Client clientThatShouldBeCreated = RandomUtils.generateRandomClient().toClient();
        Client clientThatShouldNotBeCreated = RandomUtils.generateRandomClient().toClient();

        String serverName = InProcessServerBuilder.generateName();

        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new AdminPortalServer.AdminPortalImpl()).build().start());

        AdminPortalGrpc.AdminPortalBlockingStub blockingStub = AdminPortalGrpc.newBlockingStub(
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

        Reply reply = blockingStub.createClient(clientThatShouldBeCreated);
        Thread.sleep(TOLERANCE_MS);
        Client client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldBeCreated.getCID()).build());
        Assert.assertEquals(clientThatShouldBeCreated, client);
        Assert.assertNotEquals(clientThatShouldNotBeCreated, client);
        client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldNotBeCreated.getCID()).build());
        Assert.assertNotEquals(clientThatShouldNotBeCreated, client);
        clientThatShouldBeCreated = clientThatShouldNotBeCreated;
        reply = blockingStub.createClient(clientThatShouldBeCreated);
        Thread.sleep(TOLERANCE_MS);
        Assert.assertNotEquals(clientThatShouldBeCreated, client);
        client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldBeCreated.getCID()).build());
        Assert.assertEquals(clientThatShouldBeCreated, client);

        clientThatShouldNotBeCreated = RandomUtils.generateRandomClient().toClient();
        ClientNative clientNativeThatShouldBeUpdated = ClientNative.fromClient(clientThatShouldBeCreated);
        clientNativeThatShouldBeUpdated.setZipCode("326432");
        reply = blockingStub.updateClient(clientNativeThatShouldBeUpdated.toClient());
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);
        client = blockingStub.retrieveClient(ID.newBuilder().setID(clientNativeThatShouldBeUpdated.getCID()).build());
        Assert.assertEquals(clientNativeThatShouldBeUpdated.toClient(), client);

        reply = blockingStub.deleteClient(ID.newBuilder().setID(clientNativeThatShouldBeUpdated.getCID()).build());
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        client = blockingStub.retrieveClient(ID.newBuilder().setID(clientNativeThatShouldBeUpdated.getCID()).build());
        Assert.assertNotEquals(clientNativeThatShouldBeUpdated.toClient(), client);
    }

    @Test
    public void shouldCrudProductOneServer() throws IOException, InterruptedException {
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

        productThatShouldNotBeCreated = RandomUtils.generateRandomProduct().toProduct();
        ProductNative productNativeThatShouldBeUpdated = ProductNative.fromProduct(productThatShouldBeCreated);
        productNativeThatShouldBeUpdated.setDescription("dkjshabuipokejxm");
        reply = blockingStub.updateProduct(productNativeThatShouldBeUpdated.toProduct());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productNativeThatShouldBeUpdated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(productNativeThatShouldBeUpdated.toProduct(), product);

        reply = blockingStub.deleteProduct(ID.newBuilder().setID(productNativeThatShouldBeUpdated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        product = blockingStub.retrieveProduct(ID.newBuilder().setID(productNativeThatShouldBeUpdated.getPID()).build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertNotEquals(productNativeThatShouldBeUpdated.toProduct(), product);
    }

    @Test
    public void shouldCrudClientMultipleServer() throws InterruptedException {
        List<String> serverNames = IntStream.range(0, 6).mapToObj(i -> InProcessServerBuilder.generateName()).toList();
        List<AdminPortalGrpc.AdminPortalBlockingStub> adminPortalBlockingStubs = new ArrayList<>();
        serverNames.forEach(serverName -> {
            try {
                grpcCleanup.register(InProcessServerBuilder
                        .forName(serverName).directExecutor().addService(new AdminPortalServer.AdminPortalImpl()).build().start());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            adminPortalBlockingStubs.add(
                    AdminPortalGrpc.newBlockingStub(
                            grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()))
            );
        });

        Client clientThatShouldBeCreated = RandomUtils.generateRandomClient().toClient();
        Client clientThatShouldNotBeCreated = RandomUtils.generateRandomClient().toClient();

        Reply reply = adminPortalBlockingStubs.get(0).createClient(clientThatShouldBeCreated);
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);
        adminPortalBlockingStubs.forEach(blockingStub -> {
            Client client = blockingStub.retrieveClient(ID.newBuilder().setID(clientThatShouldBeCreated.getCID()).build());
            Assert.assertEquals(clientThatShouldBeCreated, client);
        });
        ClientNative anotherClientNativeThatShouldBeCreated = RandomUtils.generateRandomClient();
        Client anotherClientThatShouldBeCreated = anotherClientNativeThatShouldBeCreated.toClient();
        reply = adminPortalBlockingStubs.get(0).createClient(anotherClientThatShouldBeCreated);
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);
        adminPortalBlockingStubs.forEach(blockingStub -> {
            Client client = blockingStub.retrieveClient(ID.newBuilder().setID(anotherClientThatShouldBeCreated.getCID()).build());
            Assert.assertEquals(client, anotherClientThatShouldBeCreated);
        });
        Client anotherClientThatShouldBeUpdated = anotherClientNativeThatShouldBeCreated.toBuilder()
                .zipCode("zipMUDADO1222")
                .build()
                .toClient();
        reply = adminPortalBlockingStubs.get(0).updateClient(anotherClientThatShouldBeUpdated);
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        adminPortalBlockingStubs.forEach(blockingStub -> {
            Client client = blockingStub.retrieveClient(ID.newBuilder().setID(anotherClientThatShouldBeUpdated.getCID()).build());
            try {
                Thread.sleep(TOLERANCE_MS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            Assert.assertEquals(client, anotherClientThatShouldBeUpdated);
        });
        reply = adminPortalBlockingStubs.get(new Random().nextInt(adminPortalBlockingStubs.size()))
                .deleteClient(ID.newBuilder()
                        .setID(anotherClientThatShouldBeUpdated.getCID())
                        .build());
        Thread.sleep(TOLERANCE_MS);
        Assert.assertEquals(reply.getError(), ReplyNative.SUCESSO.getError());
        adminPortalBlockingStubs.forEach(blockingStub -> {
            Client client = blockingStub.retrieveClient(ID.newBuilder().setID(anotherClientThatShouldBeUpdated.getCID()).build());
            Assert.assertEquals(client.getCID(), "0");
        });
    }
}
