import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import ufu.davigabriel.client.AdminPortalClient;
import ufu.davigabriel.client.OrderPortalClient;
import ufu.davigabriel.models.*;
import ufu.davigabriel.server.AdminPortalGrpc;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.OrderPortalGrpc;
import ufu.davigabriel.server.Product;
import utils.RandomOrderTriple;
import utils.RandomUtils;

import java.util.ArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

/*
Aqui ocorre

java.lang.RuntimeException: ManagedChannel allocation site

mas pode ignorar.
 */

/*
Para executar estes teste, basta executar o mosquitto e o arquivo "servers_start
.sh". Isso se deve ao fato de que as funcionalidades presentes no
OrderPortalServer dependem, em partes, das funcionalidades do AdminPortalServer.
 */
@Ignore
public class OrderPortalServerTest {

    public static int TOLERANCE_MS = 350;

    public OrderPortalGrpc.OrderPortalBlockingStub getOrderBlockingStub() {
        return OrderPortalGrpc.newBlockingStub(Grpc.newChannelBuilder(String.format("127.0.0.1:%d", GlobalVarsService.ORDER_PORTAL_SERVER_BASE_PORT), InsecureChannelCredentials.create()).build());
    }

    public AdminPortalGrpc.AdminPortalBlockingStub getAdminBlockingStub() {
        return AdminPortalGrpc.newBlockingStub(Grpc.newChannelBuilder(String.format("127.0.0.1:%d", GlobalVarsService.ADMIN_PORTAL_SERVER_BASE_PORT), InsecureChannelCredentials.create()).build());
    }

    @Test
    public void shouldCreateRandomValidOrders() throws InterruptedException {
        AdminPortalGrpc.AdminPortalBlockingStub adminPortalBlockingStub = getAdminBlockingStub();
        Thread.sleep(TOLERANCE_MS);
        OrderPortalGrpc.OrderPortalBlockingStub orderPortalBlockingStub = getOrderBlockingStub();
        Thread.sleep(TOLERANCE_MS);

        RandomOrderTriple randomOrderTriple = RandomUtils.generateRandomValidOrder();

        Assert.assertEquals(adminPortalBlockingStub.createClient(randomOrderTriple.getRandomClientNative().toClient()).getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);
        for (ProductNative productNative : randomOrderTriple.getRandomProductsNative()) {
            Assert.assertEquals(adminPortalBlockingStub.createProduct(productNative.toProduct()).getError(), ReplyNative.SUCESSO.getError());
            Thread.sleep(TOLERANCE_MS);
        }
        Assert.assertEquals(orderPortalBlockingStub.createOrder(randomOrderTriple.getRandomOrderNative().toOrder()).getError(), ReplyNative.SUCESSO.getError());
    }

    @Test
    public void shouldNotAllowUnauthorized() throws InterruptedException {
        AdminPortalGrpc.AdminPortalBlockingStub adminPortalBlockingStub = getAdminBlockingStub();
        Thread.sleep(TOLERANCE_MS);
        OrderPortalGrpc.OrderPortalBlockingStub orderPortalBlockingStub = getOrderBlockingStub();
        Thread.sleep(TOLERANCE_MS);

        RandomOrderTriple randomOrderTriple = RandomUtils.generateRandomValidOrder();

        Assert.assertEquals(adminPortalBlockingStub.createClient(randomOrderTriple.getRandomClientNative().toClient()).getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);
        for (ProductNative productNative : randomOrderTriple.getRandomProductsNative()) {
            Assert.assertEquals(adminPortalBlockingStub.createProduct(productNative.toProduct()).getError(), ReplyNative.SUCESSO.getError());
            Thread.sleep(TOLERANCE_MS);
        }
        Assert.assertEquals(orderPortalBlockingStub.createOrder(randomOrderTriple.getRandomOrderNative().toOrder()).getError(), ReplyNative.SUCESSO.getError());
        Assert.assertEquals(orderPortalBlockingStub.createOrder(randomOrderTriple.getRandomOrderNative().toOrder()).getError(), ReplyNative.DUPLICATA.getError());

        OrderNative orderNativeThatUserIsNotAuthenticated = randomOrderTriple.getRandomOrderNative();
        orderNativeThatUserIsNotAuthenticated.setOID(randomOrderTriple.getRandomOrderNative().getOID());
        orderNativeThatUserIsNotAuthenticated.setCID(Integer.toString(Integer.parseInt(randomOrderTriple.getRandomOrderNative().getCID()) + 1));
        orderNativeThatUserIsNotAuthenticated.setUpdatedVersionHash(randomOrderTriple.getRandomOrderNative().getHash());
        Assert.assertEquals(orderPortalBlockingStub.updateOrder(orderNativeThatUserIsNotAuthenticated.toOrder()).getError(), ReplyNative.NAO_LOGADO.getError());
        Thread.sleep(TOLERANCE_MS);
    }

    @Test
    public void shouldNotAllowDuplicates() throws InterruptedException {
        AdminPortalGrpc.AdminPortalBlockingStub adminPortalBlockingStub = getAdminBlockingStub();
        Thread.sleep(TOLERANCE_MS);
        OrderPortalGrpc.OrderPortalBlockingStub orderPortalBlockingStub = getOrderBlockingStub();
        Thread.sleep(TOLERANCE_MS);

        RandomOrderTriple randomOrderTriple = RandomUtils.generateRandomValidOrder();

        Assert.assertEquals(adminPortalBlockingStub.createClient(randomOrderTriple.getRandomClientNative().toClient()).getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);
        for (ProductNative productNative : randomOrderTriple.getRandomProductsNative()) {
            Assert.assertEquals(adminPortalBlockingStub.createProduct(productNative.toProduct()).getError(), ReplyNative.SUCESSO.getError());
            Thread.sleep(TOLERANCE_MS);
        }
        Assert.assertEquals(orderPortalBlockingStub.createOrder(randomOrderTriple.getRandomOrderNative().toOrder()).getError(), ReplyNative.SUCESSO.getError());

        // OID que ja existe deve dar DUPLICATA
        OrderNative orderNativeThatOIDIsRepeated = randomOrderTriple.getRandomOrderNative();
        Assert.assertEquals(orderPortalBlockingStub.createOrder(orderNativeThatOIDIsRepeated.toOrder()).getError(), ReplyNative.DUPLICATA.getError());
        Thread.sleep(TOLERANCE_MS);
    }

    @Test
    public void shouldDecreaseProductsQuantity() throws InterruptedException {
        AdminPortalGrpc.AdminPortalBlockingStub adminPortalBlockingStub = getAdminBlockingStub();
        Thread.sleep(TOLERANCE_MS);
        OrderPortalGrpc.OrderPortalBlockingStub orderPortalBlockingStub = getOrderBlockingStub();
        Thread.sleep(TOLERANCE_MS);

        RandomOrderTriple randomOrderTriple = RandomUtils.generateRandomValidOrder();

        Assert.assertEquals(adminPortalBlockingStub.createClient(randomOrderTriple.getRandomClientNative().toClient()).getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);
        for (ProductNative productNative : randomOrderTriple.getRandomProductsNative()) {
            Assert.assertEquals(adminPortalBlockingStub.createProduct(productNative.toProduct()).getError(), ReplyNative.SUCESSO.getError());
            Thread.sleep(TOLERANCE_MS);
        }
        Assert.assertEquals(randomOrderTriple.getRandomOrderNative().getCID(), randomOrderTriple.getRandomClientNative().getCID());
        Assert.assertEquals(orderPortalBlockingStub.createOrder(randomOrderTriple.getRandomOrderNative().toOrder()).getError(), ReplyNative.SUCESSO.getError());

        Thread.sleep(TOLERANCE_MS);
        for (ProductNative productNative : randomOrderTriple.getRandomProductsNative()) {
            Product productAfterUpdate = adminPortalBlockingStub.retrieveProduct(ID.newBuilder().setID(productNative.getPID()).build());
            Assert.assertNotEquals(productAfterUpdate.getPID(), "0");

            Optional<OrderItemNative> optionalOrderItemNativeThatMatchesProductNative = randomOrderTriple.getRandomOrderNative().getProducts().stream().filter(productsInOrder -> productsInOrder.getPID() == productNative.getPID()).findFirst();

            Assert.assertTrue(optionalOrderItemNativeThatMatchesProductNative.isPresent());

            OrderItemNative orderItemNativeThatMatchesProductNative = optionalOrderItemNativeThatMatchesProductNative.get();

            Assert.assertEquals(ProductNative.fromProduct(productAfterUpdate).getQuantity() + orderItemNativeThatMatchesProductNative.getQuantity(), productNative.getQuantity());
            Thread.sleep(TOLERANCE_MS);
        }
    }

    @Test
    public void shouldIncreaseProductsQuantityOnDeletion() throws InterruptedException {
        AdminPortalGrpc.AdminPortalBlockingStub adminPortalBlockingStub = getAdminBlockingStub();
        Thread.sleep(TOLERANCE_MS);
        OrderPortalGrpc.OrderPortalBlockingStub orderPortalBlockingStub = getOrderBlockingStub();
        Thread.sleep(TOLERANCE_MS);

        RandomOrderTriple randomOrderTriple = RandomUtils.generateRandomValidOrder();

        Assert.assertEquals(adminPortalBlockingStub.createClient(randomOrderTriple.getRandomClientNative().toClient()).getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);
        for (ProductNative productNative : randomOrderTriple.getRandomProductsNative()) {
            Assert.assertEquals(adminPortalBlockingStub.createProduct(productNative.toProduct()).getError(), ReplyNative.SUCESSO.getError());
            Thread.sleep(TOLERANCE_MS);
        }
        Assert.assertEquals(orderPortalBlockingStub.createOrder(randomOrderTriple.getRandomOrderNative().toOrder()).getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);

        for (ProductNative productNative : randomOrderTriple.getRandomProductsNative()) {
            Product productAfterUpdate = adminPortalBlockingStub.retrieveProduct(ID.newBuilder().setID(productNative.getPID()).build());
            Assert.assertNotEquals(productAfterUpdate.getPID(), "0");

            Optional<OrderItemNative> optionalOrderItemNativeThatMatchesProductNative = randomOrderTriple.getRandomOrderNative().getProducts().stream().filter(productsInOrder -> productsInOrder.getPID() == productNative.getPID()).findFirst();

            Assert.assertTrue(optionalOrderItemNativeThatMatchesProductNative.isPresent());

            OrderItemNative orderItemNativeThatMatchesProductNative = optionalOrderItemNativeThatMatchesProductNative.get();

            Assert.assertEquals(ProductNative.fromProduct(productAfterUpdate).getQuantity() + orderItemNativeThatMatchesProductNative.getQuantity(), productNative.getQuantity());
            Thread.sleep(TOLERANCE_MS);
        }

        Assert.assertEquals(orderPortalBlockingStub.deleteOrder(ID.newBuilder()
                        .setID(randomOrderTriple.getRandomOrderNative().getOID()).build())
                .getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);

        for (ProductNative productNative : randomOrderTriple.getRandomProductsNative()) {
            Product productAfterUpdate = adminPortalBlockingStub.retrieveProduct(ID.newBuilder().setID(productNative.getPID()).build());
            Assert.assertNotEquals(productAfterUpdate.getPID(), "0");

            Optional<OrderItemNative> optionalOrderItemNativeThatMatchesProductNative = randomOrderTriple.getRandomOrderNative().getProducts().stream().filter(productsInOrder -> productsInOrder.getPID() == productNative.getPID()).findFirst();

            Assert.assertTrue(optionalOrderItemNativeThatMatchesProductNative.isPresent());

            Assert.assertEquals(ProductNative.fromProduct(productAfterUpdate).getQuantity(), productNative.getQuantity());
            Thread.sleep(TOLERANCE_MS);
        }
    }

    @Test
    public void shouldChangeProductsQuantityOnUpdate() throws InterruptedException {
        AdminPortalGrpc.AdminPortalBlockingStub adminPortalBlockingStub = getAdminBlockingStub();
        Thread.sleep(TOLERANCE_MS);
        OrderPortalGrpc.OrderPortalBlockingStub orderPortalBlockingStub = getOrderBlockingStub();
        Thread.sleep(TOLERANCE_MS);

        RandomOrderTriple randomOrderTriple = RandomUtils.generateRandomValidOrder();

        int IN_ORDER = 2;
        int IN_PRODUCT = 3;
        int IN_ORDER_AFTER = 1;

        randomOrderTriple.getRandomOrderNative().setProducts(randomOrderTriple.getRandomOrderNative().getProducts().stream().map(orderItemNative -> {
            orderItemNative.setQuantity(IN_ORDER);
            return orderItemNative;
        }).collect(Collectors.toCollection(ArrayList::new)));

        Assert.assertEquals(adminPortalBlockingStub.createClient(randomOrderTriple.getRandomClientNative().toClient()).getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);
        for (ProductNative productNative : randomOrderTriple.getRandomProductsNative()) {
            productNative.setQuantity(IN_PRODUCT);
            Assert.assertEquals(adminPortalBlockingStub.createProduct(productNative.toProduct()).getError(), ReplyNative.SUCESSO.getError());
            Thread.sleep(TOLERANCE_MS);
        }
        Assert.assertEquals(orderPortalBlockingStub.createOrder(randomOrderTriple.getRandomOrderNative().toOrder()).getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);

        randomOrderTriple.getRandomOrderNative().setUpdatedVersionHash(randomOrderTriple.getRandomOrderNative().getHash());

        randomOrderTriple.getRandomOrderNative().setProducts(randomOrderTriple.getRandomOrderNative().getProducts().stream().map(orderItemNative -> {
            orderItemNative.setQuantity(IN_ORDER_AFTER);
            return orderItemNative;
        }).collect(Collectors.toCollection(ArrayList::new)));

        Assert.assertEquals(orderPortalBlockingStub.updateOrder(randomOrderTriple.getRandomOrderNative().toOrder()).getError(), ReplyNative.SUCESSO.getError());
        Thread.sleep(TOLERANCE_MS);

        for (ProductNative productNative : randomOrderTriple.getRandomProductsNative()) {
            Product productAfterUpdate = adminPortalBlockingStub.retrieveProduct(ID.newBuilder().setID(productNative.getPID()).build());
            Assert.assertNotEquals(productAfterUpdate.getPID(), "0");

            Assert.assertEquals(ProductNative.fromProduct(productAfterUpdate).getQuantity(), IN_PRODUCT - IN_ORDER_AFTER);
        }
    }
}
