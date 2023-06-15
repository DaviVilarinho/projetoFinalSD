package ufu.davigabriel.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufu.davigabriel.exceptions.*;
import ufu.davigabriel.models.OrderNative;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Order;
import ufu.davigabriel.server.distributedDatabase.RatisClient;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class OrderUpdaterMiddleware extends UpdaterMiddleware implements IOrderProxyDatabase {
    private static OrderUpdaterMiddleware instance;
    private OrderCacheService orderCacheService = OrderCacheService.getInstance();
    private Logger logger = LoggerFactory.getLogger(OrderUpdaterMiddleware.class);

    public static OrderUpdaterMiddleware getInstance() {
        if (instance == null) {
            instance = new OrderUpdaterMiddleware();
        }
        return instance;
    }
    @Override
    public String getSelfSavePath() {
        return "orders/";
    }

    public String getOrderStorePath(Order order) {
        return super.getStorePath(order.getOID());
    }

    public String getClientOrdersBaseSavePath() { return "client_orders/"; }
    public String getClientOrdersStorePath(String clientId) { return getClientOrdersBaseSavePath() + clientId; }

    private RatisClient getRatisClientFromOrder(Order order) {
        return super.getRatisClients()[Integer.parseInt(order.getOID()) % 2];
    }

    @Override
    protected RatisClient getRatisClientFromID(String id) {
        return super.getRatisClients()[Integer.parseInt(id) % 2];
    }

    public void throwIfClientUnauthorized(Order order) throws UnauthorizedUserException {
        throw new UnauthorizedUserException();
    }

    @Override
    public void createOrder(Order order) throws DuplicatePortalItemException, RatisClientException, BadRequestException, UnauthorizedUserException {
        throwIfClientUnauthorized(order);
        if (orderCacheService.hasOrder(order)) {
            throw new DuplicatePortalItemException("Order já existe: " + order);
        }
        try {
            if (!getRatisClientFromOrder(order).add(getOrderStorePath(order), OrderNative.fromOrder(order).toJson()).isSuccess()) {
                throw new DuplicatePortalItemException();
            }
            orderCacheService.createOrder(order);
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            logger.debug("Erro json inválido: " + order);
            throw new BadRequestException();
        }
        addClientOrderId(order);
    }

    @Override
    public void updateOrder(Order order) throws NotFoundItemInPortalException, RatisClientException, BadRequestException, UnauthorizedUserException {
        throwIfClientUnauthorized(order);
        if (!orderCacheService.hasOrder(order)) {
            retrieveOrder(order);
        }
        try {
            throwIfClientUnauthorized(retrieveOrder(order));
            if (!getRatisClientFromOrder(order).update(
                    getOrderStorePath(order),
                    order.toString()).isSuccess()) {
                throw new NotFoundItemInPortalException();
            }
            orderCacheService.updateOrder(order);
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            logger.debug("Erro json inválido: " + order);
            throw new BadRequestException();
        }
    }

    public Order retrieveOrder(Order order) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        return retrieveOrder(ID.newBuilder().setID(order.getOID()).build());
    }

    @Override
    public Order retrieveOrder(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        try {
            return orderCacheService.retrieveOrder(id);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            logger.debug("ID não encontrado, tentando buscar no bd " + id);
        }

        try {
            String queryOrder = getRatisClientFromID(id.getID()).get(getStorePath(id.getID())).getMessage().getContent().toString(Charset.defaultCharset());
            Order order = OrderNative.fromJson(queryOrder).toOrder();
            orderCacheService.createOrder(order);
            return order;
        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new NotFoundItemInPortalException();
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            logger.debug("Erro json inválido: " + id);
            throw new BadRequestException();
        } catch (DuplicatePortalItemException e) {
            e.printStackTrace();
            throw new RatisClientException("Erro de sincronizacao interna com a database " + id.getID());
        }
    }

    @Override
    public void deleteOrder(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        if (!orderCacheService.hasOrder(id.getID())) {
            retrieveOrder(id);
        }
        try {
            if (!getRatisClientFromID(id.getID()).del(getStorePath(id.getID())).isSuccess()) {
                throw new NotFoundItemInPortalException();
            }
            orderCacheService.deleteOrder(id);
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            logger.debug("Erro json inválido: " + id);
            throw new BadRequestException();
        }
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
            logger.debug("Não havia na cache os dados do cliente " + id);
        }
        try {
            String query = getRatisClientFromID(id.getID()).get(id.getID()).getMessage().getContent().toString(Charset.defaultCharset());
            ArrayList<String> clientOrders = new Gson().fromJson(query, new TypeToken<ArrayList<String>>() {}.getType());
            orderCacheService.updateClientOrders(id.getID(), clientOrders);
            return clientOrders;
        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new NotFoundItemInPortalException();
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            logger.debug("Erro json inválido: " + id);
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
            logger.debug("Cliente não havia ordens");
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
            logger.debug("Cliente não havia ordens, nada a remover");
            clientOrders = new ArrayList<>();
        }
        clientOrders.remove(orderId);
        pushClientOrders(clientId, clientOrders);
    }

}
