package ufu.davigabriel.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufu.davigabriel.exceptions.*;
import ufu.davigabriel.models.GlobalVarsService;
import ufu.davigabriel.models.OrderNative;
import ufu.davigabriel.models.ProductNative;
import ufu.davigabriel.server.*;
import ufu.davigabriel.server.distributedDatabase.RatisClient;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Optional;

public class OrderUpdaterMiddleware extends UpdaterMiddleware implements IOrderProxyDatabase {
    private static OrderUpdaterMiddleware instance;
    private OrderCacheService orderCacheService = OrderCacheService.getInstance();
    private Logger logger = LoggerFactory.getLogger(OrderUpdaterMiddleware.class);
    private static AdminPortalGrpc.AdminPortalBlockingStub adminBlockingStub;

    private OrderUpdaterMiddleware() {
        String address = String.format("localhost:%d", GlobalVarsService.getInstance().getRandomAdminPortalPort());
        System.out.println("Connecting to AdminPortalServer at " + address);
        adminBlockingStub = AdminPortalGrpc.newBlockingStub(
                Grpc.newChannelBuilder(address, InsecureChannelCredentials.create()).build());
    }

    public static OrderUpdaterMiddleware getInstance() {
        if (instance == null) {
            instance = new OrderUpdaterMiddleware();
        }
        return instance;
    }

    private void changeGlobalProductQuantity(String productId, int variation) {
        try {
            ProductNative productToBeAdjusted = ProductNative.fromProduct(adminBlockingStub.retrieveProduct(ID.newBuilder().setID(productId).build()));
            productToBeAdjusted.setUpdatedVersionHash(productToBeAdjusted.getHash());
            productToBeAdjusted.setQuantity(productToBeAdjusted.getQuantity() + variation);
            Reply reply = adminBlockingStub.updateProduct(productToBeAdjusted.toProduct());
            System.out.println("O status da atualização do produto " + productId + ": " + reply.getDescription());
        } catch (Exception exception) {
            exception.printStackTrace();
            System.err.println("Não foi possível atualizar " + productId + " para uma variação de " + variation);
        }
    }

    public void authenticateClient(String CID) throws UnauthorizedUserException {
        Client client = adminBlockingStub.retrieveClient(ID.newBuilder().setID(CID).build());

        System.out.println("Encontrado um cliente do portal admin: " + client.toString());

        if("0".equals(client.getCID())) throw new UnauthorizedUserException();
    }

    @Override
    public String getSelfSavePath() {
        return "orders/";
    }

    public String getOrderStorePath(Order order) {
        return super.getStorePath(order.getOID());
    }

    public String getClientOrdersBaseSavePath() {return "client_orders/";}

    public String getClientOrdersStorePath(String clientId) {return getClientOrdersBaseSavePath() + clientId;}

    private RatisClient getRatisClientFromOrder(Order order) {
        return super.getRatisClients()[Integer.parseInt(order.getOID()) % 2];
    }

    @Override
    protected RatisClient getRatisClientFromID(String id) {
        return super.getRatisClients()[Integer.parseInt(id) % 2];
    }

    public void throwIfCIDIsDifferentFromOldOrderCID(Optional<Order> optionalOldOrder, Order newOrder) throws UnauthorizedUserException {
        if (optionalOldOrder.isPresent()) {
            if (!optionalOldOrder.get().getCID().equals(newOrder.getCID())) {
                throw new UnauthorizedUserException();
            }
        }
    }

    @Override
    public void createOrder(Order order) throws DuplicatePortalItemException, RatisClientException, BadRequestException, UnauthorizedUserException {
        authenticateClient(order.getCID());
        try {
            retrieveOrder(order);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            System.out.println("Não havia ordem " + order.getOID() + ", prosseguindo criação");
        }
        if (orderCacheService.hasOrder(order)) {
            throw new DuplicatePortalItemException("Order já existe: " + order);
        }
        try {
            if (!getRatisClientFromOrder(order).add(getOrderStorePath(order),
                                                    new Gson().toJson(order)).isSuccess()) {
                throw new DuplicatePortalItemException();
            }
            orderCacheService.createOrder(order);
        } catch (IllegalStateException | NumberFormatException | NullPointerException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + order);
            throw new BadRequestException();
        }
        addClientOrderId(order);
        OrderNative.fromOrder(order).getProducts().forEach(orderItemNative -> changeGlobalProductQuantity(orderItemNative.getPID(), -orderItemNative.getQuantity()));
    }

    @Override
    public void updateOrder(Order order) throws NotFoundItemInPortalException, RatisClientException, BadRequestException, UnauthorizedUserException, IllegalVersionPortalItemException {
        Optional<Order> oldOrder = Optional.of(retrieveOrder(order));
        authenticateClient(order.getCID());
        orderCacheService.throwIfNotUpdatable(OrderNative.fromOrder(order));
        throwIfCIDIsDifferentFromOldOrderCID(oldOrder, order);
        OrderNative newOrderNative = OrderNative.fromOrder(order);
        newOrderNative.getProducts().removeIf(item -> item.getQuantity() == 0);
        try {
            order = newOrderNative.toOrder();
            if (!getRatisClientFromOrder(order).update(
                    getOrderStorePath(order),
                    new Gson().toJson(order)).isSuccess()) {
                throw new NotFoundItemInPortalException();
            }
            orderCacheService.updateOrder(order);
        } catch (IllegalStateException | NumberFormatException | NullPointerException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + order);
            throw new BadRequestException();
        }
        HashMap<String, Integer> auxiliarHashMapForProductQuantityRestoration = new HashMap<>();
        OrderNative oldOrderNative = OrderNative.fromOrder(oldOrder.get());
        oldOrderNative.getProducts().forEach(oldItem -> auxiliarHashMapForProductQuantityRestoration.put(
                oldItem.getPID(),
                auxiliarHashMapForProductQuantityRestoration.getOrDefault(oldItem.getPID(), 0) + oldItem.getQuantity()));
        newOrderNative.getProducts().forEach(newItem -> auxiliarHashMapForProductQuantityRestoration.put(
                newItem.getPID(),
                auxiliarHashMapForProductQuantityRestoration.getOrDefault(newItem.getPID(), 0) - newItem.getQuantity()));
        auxiliarHashMapForProductQuantityRestoration.forEach(this::changeGlobalProductQuantity);
    }

    public Order retrieveOrder(Order order) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        return retrieveOrder(ID.newBuilder().setID(order.getOID()).build());
    }

    @Override
    public Order retrieveOrder(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        try {
            return orderCacheService.retrieveOrder(id);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            System.out.println("ID não encontrado, tentando buscar no bd " + id);
        }

        try {
            String queryOrder = getRatisClientFromID(id.getID()).get(getStorePath(id.getID())).getMessage().getContent().toString(Charset.defaultCharset());
            System.out.println("Banco encontrou order: " + queryOrder);
            String onlyJson = queryOrder.split(":", 2)[1];
            if ("null".equals(onlyJson)) {
                throw new NotFoundItemInPortalException(id.getID());
            }
            Order order = new Gson().fromJson(onlyJson, Order.class);
            orderCacheService.createOrder(order);
            return order;
        } catch (JsonSyntaxException | ArrayIndexOutOfBoundsException jsonSyntaxException) {
            throw new NotFoundItemInPortalException();
        } catch (IllegalStateException | NumberFormatException | NullPointerException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + id);
            throw new BadRequestException();
        } catch (DuplicatePortalItemException e) {
            e.printStackTrace();
            throw new RatisClientException("Erro de sincronizacao interna com a database " + id.getID());
        }
    }

    @Override
    public void deleteOrder(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        Order toDeleteOrder = retrieveOrder(id);
        try {
            if (!getRatisClientFromID(id.getID()).del(getStorePath(id.getID())).isSuccess()) {
                throw new NotFoundItemInPortalException();
            }
            orderCacheService.deleteOrder(id);
        } catch (IllegalStateException | NumberFormatException | NullPointerException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + id);
            throw new BadRequestException();
        }
        OrderNative.fromOrder(toDeleteOrder).getProducts().forEach(orderItemNative -> {
            changeGlobalProductQuantity(orderItemNative.getPID(), orderItemNative.getQuantity());
        });
    }

    @Override
    public ArrayList<Order> retrieveClientOrders(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        ArrayList<Order> clientOrders = new ArrayList<>();
        for (String orderId : retrieveClientOrdersIds(id)) {
            Order orderFromOrderId = retrieveOrder(ID.newBuilder().setID(orderId).build());
            clientOrders.add(orderFromOrderId);
        }
        return clientOrders;
    }

    private ArrayList<String> retrieveClientOrdersIds(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        try {
            return orderCacheService.retrieveClientOrdersIds(id);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            System.out.println("Não havia na cache os dados do cliente " + id);
        }
        try {
            String query = getRatisClientFromID(id.getID()).get(id.getID()).getMessage().getContent().toString(Charset.defaultCharset());
            ArrayList<String> clientOrders = new Gson().fromJson(query, new TypeToken<ArrayList<String>>() {
            }.getType());
            orderCacheService.updateClientOrders(id.getID(), clientOrders);
            return clientOrders;
        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new NotFoundItemInPortalException();
        } catch (IllegalStateException | NumberFormatException | NullPointerException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + id);
            throw new BadRequestException();
        }
    }

    private void addClientOrderId(Order order) throws RatisClientException {
        addClientOrderId(order.getCID(), order.getOID());
    }

    private void addClientOrderId(ID clientId, ID orderId) throws RatisClientException {
        addClientOrderId(clientId.getID(), orderId.getID());
    }

    private void pushClientOrders(String clientId, ArrayList<String> orderIds) throws RatisClientException {
        if (!getRatisClientFromID(clientId)
                .update(clientId, new Gson().toJson(orderIds))
                .isSuccess()) {
            throw new RatisClientException();
        }
    }

    @Override
    public void addClientOrderId(String clientId, String orderId) throws RatisClientException {
        ArrayList<String> clientOrders;
        try {
            clientOrders = retrieveClientOrdersIds(ID.newBuilder().setID(clientId).build());
        } catch (NotFoundItemInPortalException | BadRequestException notFoundItemInPortalException) {
            System.out.println("Cliente não havia ordens");
            clientOrders = new ArrayList<>();
        }
        clientOrders.add(orderId);
        pushClientOrders(clientId, clientOrders);
    }

    @Override
    public void removeClientOrderId(String clientId, String orderId) throws RatisClientException, BadRequestException {
        ArrayList<String> clientOrders;
        try {
            clientOrders = retrieveClientOrdersIds(ID.newBuilder().setID(clientId).build());
        } catch (NotFoundItemInPortalException | RatisClientException notFoundItemInPortalException) {
            System.out.println("Cliente não havia ordens, nada a remover");
            clientOrders = new ArrayList<>();
        }
        clientOrders.remove(orderId);
        pushClientOrders(clientId, clientOrders);
    }
}
