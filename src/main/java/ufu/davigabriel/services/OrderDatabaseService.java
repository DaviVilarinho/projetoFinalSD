package ufu.davigabriel.services;
//todo database validator

import ufu.davigabriel.exceptions.DuplicatePortalItemException;
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
public class OrderDatabaseService implements IOrderProxyDatabase {
    private static OrderDatabaseService instance;
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

    private OrderDatabaseService() {
        if (instance == null) {
            ordersMap = new HashMap<>();
            clientOrdersMap = new HashMap<>();
        }
    }

    public static OrderDatabaseService getInstance() {
        if (instance == null) {
            instance = new OrderDatabaseService();
        }
        return instance;
    }

    public void listAll() {
        ordersMap.forEach((s, orderNative) -> System.out.println(orderNative));
    }

    public void createOrder(OrderNative orderNative) throws DuplicatePortalItemException {
        if (hasOrder(orderNative.getOID()))
            throw new DuplicatePortalItemException();
        ordersMap.put(orderNative.getOID(), orderNative.toJson());
        addClientOrderId(orderNative.getCID(), orderNative.getOID());
    }

    public void createOrder(Order order) throws DuplicatePortalItemException {
        createOrder(OrderNative.fromOrder(order));
    }

    public OrderNative retrieveOrder(String id) throws NotFoundItemInPortalException {
        if (!hasOrder(id)) throw new NotFoundItemInPortalException();
        return OrderNative.fromJson(ordersMap.get(id));
    }

    public OrderNative retrieveOrder(ID id) throws NotFoundItemInPortalException {
        return retrieveOrder(id.getID());
    }

    public void updateOrder(OrderNative orderNative) throws NotFoundItemInPortalException {
        if (!hasOrder(orderNative.getOID()))
            throw new NotFoundItemInPortalException();
        ordersMap.put(orderNative.getOID(), orderNative.toJson());
    }

    public void updateOrder(Order order) throws NotFoundItemInPortalException {
        updateOrder(OrderNative.fromOrder(order));
    }

    public void deleteOrder(String id) throws NotFoundItemInPortalException {
        if (!hasOrder(id)) throw new NotFoundItemInPortalException();
        OrderNative removedOrder = OrderNative.fromJson(ordersMap.remove(id));
        removeClientOrderId(removedOrder.getCID(), removedOrder.getOID());
    }

    public void deleteOrder(ID id) throws NotFoundItemInPortalException {
        deleteOrder(id.getID());
    }

    public ArrayList<OrderNative> retrieveClientOrders(ID id) throws NotFoundItemInPortalException {
        if (!hasClient(id.getID())) throw new NotFoundItemInPortalException();
        return new ArrayList<>(clientOrdersMap
                .get(id.getID())
                .stream().map(orderIdFromClient -> OrderNative.fromJson(ordersMap.get(orderIdFromClient)))
                .toList());
    }

    public boolean hasOrder(String id) {
        return ordersMap.containsKey(id);
    }

    public boolean hasClient(String id) {
        return clientOrdersMap.containsKey(id);
    }

    public void addClientOrderId(String clientId, String orderId) {
        clientOrdersMap.putIfAbsent(clientId, new ArrayList<>());
        clientOrdersMap.get(clientId).add(orderId);
    }

    public void removeClientOrderId(String clientId, String orderId) {
        clientOrdersMap.put(clientId,
                clientOrdersMap.get(clientId).stream()
                        .filter(order -> !order.equals(orderId))
                        .collect(Collectors.toCollection(ArrayList::new)));
    }
}
