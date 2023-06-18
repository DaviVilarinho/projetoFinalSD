package ufu.davigabriel.services;

import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.IllegalVersionPortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.models.OrderNative;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

/**
 * As mudancas de pedidos devem ser realizadas aqui, bem como
 * o armazenamento das tabelas.
 * <p>
 * Esta classe nao tem responsabilidade de sincronia ou atualizacao,
 * apenas realiza mudancas na tabela e nao permite estados invalidos.
 */
public class OrderCacheService extends BaseCacheService implements IOrderProxyDatabase {
    private static OrderCacheService instance;
    /*
    O esquema de dados no ordersMap ocorre da seguinte maneira: String (ID) ->
    String (JSON) representando uma order.
    */
    private HashMap<String, String> ordersMap;
    private HashMap<String, ArrayList<String>> clientOrdersMap;
    /*
    A motivacao de um clientOrdersMap decorre do fato de que tanto filtrar
    ordens por cliente eh ruim
    quanto
    filtrar todos clientes por ordens eh ruim

    entao vale a dor de cabeca :)
     */

    private OrderCacheService() {
        if (instance == null) {
            ordersMap = new HashMap<>();
            clientOrdersMap = new HashMap<>();
        }
    }

    public static OrderCacheService getInstance() {
        if (instance == null) {
            instance = new OrderCacheService();
        }
        return instance;
    }

    public void listAll() {
        ordersMap.forEach((s, orderNative) -> System.out.println(orderNative));
    }

    private void createOrder(OrderNative orderNative) throws DuplicatePortalItemException {
        if (hasOrder(orderNative.getOID()))
            throw new DuplicatePortalItemException();
        this.addToCache(orderNative);
        ordersMap.put(orderNative.getOID(), orderNative.toJson());
        addClientOrderId(orderNative.getCID(), orderNative.getOID());
    }

    public void createOrder(Order order) throws DuplicatePortalItemException {
        createOrder(OrderNative.fromOrder(order));
    }

    private OrderNative retrieveOrder(String id) throws NotFoundItemInPortalException {
        this.throwNotFoundItemIfOldOrNotFoundHash(id);
        if (!hasOrder(id)) throw new NotFoundItemInPortalException();
        return OrderNative.fromJson(ordersMap.get(id));
    }

    public Order retrieveOrder(ID id) throws NotFoundItemInPortalException {
        return retrieveOrder(id.getID()).toOrder();
    }

    private void updateOrder(OrderNative orderNative) throws NotFoundItemInPortalException, IllegalVersionPortalItemException {
        throwIfNotUpdatable(orderNative);
        if (!hasOrder(orderNative.getOID()))
            throw new NotFoundItemInPortalException();
        ordersMap.put(orderNative.getOID(), orderNative.toJson());
        addToCache(orderNative);
    }

    public void updateOrder(Order order) throws NotFoundItemInPortalException, IllegalVersionPortalItemException {
        updateOrder(OrderNative.fromOrder(order));
    }

    private void deleteOrder(String id) throws NotFoundItemInPortalException {
        throwNotFoundItemIfOldOrNotFoundHash(id);
        if (!hasOrder(id)) throw new NotFoundItemInPortalException();
        OrderNative removedOrder = OrderNative.fromJson(ordersMap.remove(id));
        removeClientOrderId(removedOrder.getCID(), removedOrder.getOID());
        removeFromCache(id);
    }

    public void deleteOrder(ID id) throws NotFoundItemInPortalException {
        deleteOrder(id.getID());
    }

    public ArrayList<Order> retrieveClientOrders(ID id) throws NotFoundItemInPortalException {
        if (!hasClient(id.getID())) throw new NotFoundItemInPortalException();
        return new ArrayList<>(clientOrdersMap
                .get(id.getID())
                .stream().map(orderIdFromClient -> OrderNative.fromJson(ordersMap.get(orderIdFromClient)).toOrder())
                .toList());
    }

    public ArrayList<String> retrieveClientOrdersIds(ID id) throws NotFoundItemInPortalException {
        if (!hasClient(id.getID())) throw new NotFoundItemInPortalException();
        return new ArrayList<>(clientOrdersMap.get(id.getID()));
    }

    public boolean hasOrder(String id) {
        return ordersMap.containsKey(id) && !isCacheOldOrMissing(id);
    }

    public boolean hasOrder(Order order) { return hasOrder(order.getOID()); }

    public boolean hasClient(String id) {
        return clientOrdersMap.containsKey(id);
    }

    public void addClientOrderId(ID clientID, ID orderID) {
        addClientOrderId(clientID.getID(), orderID.getID());
    }

    public void updateClientOrders(String clientId, ArrayList<String> clientOrdersIds)  {
        clientOrdersMap.put(clientId, clientOrdersIds);
    }

    @Override
    public void addClientOrderId(String clientId, String orderId) {
        clientOrdersMap.putIfAbsent(clientId, new ArrayList<>());
        clientOrdersMap.get(clientId).add(orderId);
    }

    public void setClientOrderId(String clientId, ArrayList<String> orders) {
        clientOrdersMap.put(clientId, orders);
    }

    public void removeClientOrderId(ID clientID, ID orderID) {
        removeClientOrderId(clientID.getID(), orderID.getID());
    }

    @Override
    public void removeClientOrderId(String clientId, String orderId) {
        clientOrdersMap.put(clientId,
                clientOrdersMap.get(clientId).stream()
                        .filter(order -> !order.equals(orderId))
                        .collect(Collectors.toCollection(ArrayList::new)));
    }
}
