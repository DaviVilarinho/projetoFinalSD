package ufu.davigabriel.services;

import com.google.gson.Gson;
import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.exceptions.RatisClientException;
import ufu.davigabriel.models.OrderNative;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Order;

import java.nio.charset.Charset;
import java.util.ArrayList;

public class OrderUpdaterMiddleware extends UpdaterMiddleware implements IOrderProxyDatabase {
    private static OrderUpdaterMiddleware instance;
    private OrderCacheService orderCacheService = OrderCacheService.getInstance();

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

    public String getClientOrdersSavePath() { return "client_orders/"; }
    public String getClientOrdersStorePath(Order order) { return getClientOrdersSavePath() + order.getCID(); }

    @Override
    public void createOrder(Order order) throws DuplicatePortalItemException, RatisClientException {
        if (orderCacheService.hasOrder(order)) {
            throw new DuplicatePortalItemException("Pedido já existe: " + order.toString());
        }
        getRatisClientFromID(order.getOID()).add(getOrderStorePath(order),
                                                 OrderNative.fromOrder(order).toJson());

        ArrayList<Order> clientOrders = new ArrayList<>();
        try {
            clientOrders = retrieveClientOrders(ID.newBuilder().setID(order.getCID()).build());
        } catch (NotFoundItemInPortalException e) {
            //TODO
        }

    }

    @Override
    public void updateOrder(Order order) throws NotFoundItemInPortalException, RatisClientException {
        if (!orderCacheService.hasOrder(order)) {
            retrieveOrder(order); // TODO avaliar se está lançando o NOTFOUND
        }
        orderCacheService.updateOrder(OrderNative.fromJson(getRatisClientFromID(order.getOID()).update(
                getOrderStorePath(order), order.toString()).getMessage().getContent().toString(
                Charset.defaultCharset())).toOrder()); // TODO avaliar get nulo
    }

    public Order retrieveOrder(Order order) throws NotFoundItemInPortalException, RatisClientException {
        return retrieveOrder(ID.newBuilder().setID(order.getOID()).build());
    }

    @Override
    public Order retrieveOrder(ID id) throws NotFoundItemInPortalException, RatisClientException {
        try {
            return orderCacheService.retrieveOrder(id);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            Order order = OrderNative.fromJson(getRatisClientFromID(id.getID()).get(
                    getStorePath(id.getID())).getMessage().getContent().toString(
                    Charset.defaultCharset())).toOrder(); // TODO avaliar get nulo
            try {
                orderCacheService.createOrder(order);
            } catch (DuplicatePortalItemException e) {
                throw new RatisClientException("Erro de sincronizacao interna com a database " + id.getID());
            }

            return order;
        }
    }

    @Override
    public void deleteOrder(ID id) throws NotFoundItemInPortalException, RatisClientException {
        if (!orderCacheService.hasOrder(id.getID())) {
            retrieveOrder(id); // TODO avaliar se está lançando o NOTFOUND
        }
        orderCacheService.deleteOrder(ID.newBuilder().setID(OrderNative.fromJson(
                getRatisClientFromID(id.getID()).del(
                        getStorePath(id.getID().toString())).getMessage().getContent().toString(
                        Charset.defaultCharset())).getOID()).build()); // TODO avaliar get nulo
    }

    @Override
    public ArrayList<Order> retrieveClientOrders(ID id) throws NotFoundItemInPortalException, RatisClientException {
        return null; //TODO
    }
}
