package ufu.davigabriel.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufu.davigabriel.exceptions.*;
import ufu.davigabriel.models.*;
import ufu.davigabriel.server.*;
import ufu.davigabriel.server.distributedDatabase.RatisClient;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * {@link OrderUpdaterMiddleware}
 * Aqui a classe tem como objetivo controlar as requisições para a Cache e para o Ratis
 * Tem que manter controle de versão, relação de autenticação e validação se o produto atualizado está ok
 */
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

    /*  este método tem sérios problemas de eficiência e mesmo de consistência
        pelo tempo de desenvolvimento do projeto não conseguimos mudar, mas infelizmente
        no modo que está ele repete requisições no throwBadRequest e no change.

        Qualquer mudança era arriscada, então prefirimos não fazer.

        O problema de consistência está em:
        -   E se falhar?
        -   (menos pior) é possível que o client note o seguinte cenário:
            -   OrderClient ---cria order com PID 1 --> Retorna Order ok, atualiza Produto no AdminPortalServer
            -   AdminClient ---Me veja informacoes do produto 1 --> Retorna Produto Desatualizado
            isso se deve à presença de cache, mas pelos testes sempre após o TTL é validado, mas pode ser que não... Assim como a Amazon na vida real.
     */
    private void changeGlobalProductQuantity(String productId, int variation) {
        try {
            // acha produto do servidor de admin
            ProductNative productToBeAdjusted = ProductNative.fromProduct(adminBlockingStub.retrieveProduct(ID.newBuilder().setID(productId).build()));
            // pega qual hash dessa versao
            productToBeAdjusted.setUpdatedVersionHash(productToBeAdjusted.getHash());
            // atualiza pra variacao
            productToBeAdjusted.setQuantity(productToBeAdjusted.getQuantity() + variation);
            // reenvia
            Reply reply = adminBlockingStub.updateProduct(productToBeAdjusted.toProduct());
            System.out.println("O status da atualização do produto " + productToBeAdjusted.getPID() + ": " + ReplyNative.fromReply(reply).name());
        } catch (Exception exception) {
            exception.printStackTrace();
            System.err.println("Não foi possível atualizar " + productId + " para uma variação de " + variation);
        }
    }

    private ProductNative throwBadRequestIfVariationGeneratesInvalidQuantity(String productId, int variation) throws BadRequestException {
        try {
            // mesma coisa que o anterior, mas só importa variações que diminuam abaixo de zero e não chama update
            ProductNative productToBeAdjusted = ProductNative.fromProduct(adminBlockingStub.retrieveProduct(ID.newBuilder().setID(productId).build()));
            if (-variation > productToBeAdjusted.getQuantity()) { // - porque positivo é algo bom, e sempre é possível, negativo que não
                throw new BadRequestException("Essa requisição violará a quantidade do produto " + productId);
            }
            return productToBeAdjusted;
        } catch (Exception exception) {
            if (exception.getClass() != BadRequestException.class) {
                exception.printStackTrace();
                throw new BadRequestException("Não foi possível verificar variação causada pela order change request");
            }
            throw exception;
        }
    }

    public void authenticateClient(String CID) throws UnauthorizedUserException {
        Client client = adminBlockingStub.retrieveClient(ID.newBuilder().setID(CID).build());

        System.out.println("Encontrado um cliente do portal admin: " + client.toString());

        if("0".equals(client.getCID())) throw new UnauthorizedUserException(); // verifica se existe
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

    public void throwIfCIDIsDifferentFromOldOrderCID(Order optionalOldOrder, Order newOrder) throws UnauthorizedUserException {
        if (!optionalOldOrder.getCID().equals(newOrder.getCID())) {
            throw new UnauthorizedUserException();
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
            for (OrderItemNative orderItemNative : OrderNative.fromOrder(order).getProducts()) {
                throwBadRequestIfVariationGeneratesInvalidQuantity(orderItemNative.getPID(), -orderItemNative.getQuantity());
            }
            System.out.println("Nenhum conflito de quantidade em " + order.getOID() + " estimado, prosseguindo para update");
            orderCacheService.createOrder(order);
        } catch (IllegalStateException | NumberFormatException | NullPointerException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + order);
            throw new BadRequestException();
        }
        addClientOrderId(order); // lembrar de colocar na listinha de ID's
        // lembrar de atualizar produtos
        OrderNative.fromOrder(order).getProducts().forEach(orderItemNative -> changeGlobalProductQuantity(orderItemNative.getPID(), -orderItemNative.getQuantity()));
    }

    @Override
    public void updateOrder(Order order) throws NotFoundItemInPortalException, RatisClientException, BadRequestException, UnauthorizedUserException, IllegalVersionPortalItemException {
        Order oldOrder = retrieveOrder(order); // pega se existe, se não estoura
        authenticateClient(order.getCID()); // verifica que é válido CID
        orderCacheService.throwIfNotUpdatable(OrderNative.fromOrder(order)); // verifica se bate o hash com old
        throwIfCIDIsDifferentFromOldOrderCID(oldOrder, order); // nao permitir mudar cid
        OrderNative oldOrderNative = OrderNative.fromOrder(oldOrder);
        OrderNative newOrderNative = OrderNative.fromOrder(order);

        /* valida se não vai dar erro a mudança */
        HashMap<String, Integer> auxiliarHashMapForProductQuantityRestoration = new HashMap<>();
        for (OrderItemNative oldItem : oldOrderNative.getProducts()) {
            auxiliarHashMapForProductQuantityRestoration.put(
                    oldItem.getPID(),
                    auxiliarHashMapForProductQuantityRestoration.getOrDefault(oldItem.getPID(), 0) + oldItem.getQuantity());
        }
        for (OrderItemNative newItem : newOrderNative.getProducts()) {
            auxiliarHashMapForProductQuantityRestoration.put(
                    newItem.getPID(),
                    auxiliarHashMapForProductQuantityRestoration.getOrDefault(newItem.getPID(), 0) - newItem.getQuantity());
        }
        for (Map.Entry<String, Integer> productEntry : auxiliarHashMapForProductQuantityRestoration.entrySet()) {
            throwBadRequestIfVariationGeneratesInvalidQuantity(productEntry.getKey(), productEntry.getValue());
        }
        System.out.println("Nenhum conflito de quantidade em " + order.getOID() + " estimado, prosseguindo para update");
        newOrderNative.getProducts().removeIf(item -> item.getQuantity() == 0);// tirar zeros
        try {
            order = newOrderNative.toOrder();
            if (!getRatisClientFromOrder(order).update(
                    getOrderStorePath(order),
                    new Gson().toJson(order)).isSuccess()) {
                throw new NotFoundItemInPortalException();
            }
            // se deu pra colocar no Ratis, atualiza na cache
            orderCacheService.updateOrder(order);
        } catch (IllegalStateException | NumberFormatException | NullPointerException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + order);
            throw new BadRequestException();
        }
        HashMap<String, Integer> auxiliarHashMapForProductQuantityRestoration2 = new HashMap<>();
        // atualizar cada produto
        oldOrderNative.getProducts().forEach(oldItem -> auxiliarHashMapForProductQuantityRestoration2.put(
                oldItem.getPID(),
                auxiliarHashMapForProductQuantityRestoration2.getOrDefault(oldItem.getPID(), 0) + oldItem.getQuantity()));
        newOrderNative.getProducts().forEach(newItem -> auxiliarHashMapForProductQuantityRestoration2.put(
                newItem.getPID(),
                auxiliarHashMapForProductQuantityRestoration2.getOrDefault(newItem.getPID(), 0) - newItem.getQuantity()));
        auxiliarHashMapForProductQuantityRestoration2.forEach(this::changeGlobalProductQuantity);
    }

    public Order retrieveOrder(Order order) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        return retrieveOrder(ID.newBuilder().setID(order.getOID()).build());
    }

    @Override
    public Order retrieveOrder(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        // verifica se tem na cache & funciona
        try {
            return orderCacheService.retrieveOrder(id);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            System.out.println("ID não encontrado, tentando buscar no bd " + id);
        }

        // se não era possível pega do db
        try {
            String queryOrder = getRatisClientFromID(id.getID()).get(getStorePath(id.getID())).getMessage().getContent().toString(Charset.defaultCharset());
            System.out.println("Banco encontrou order: " + queryOrder);
            String onlyJson = queryOrder.split(":", 2)[1];
            if ("null".equals(onlyJson)) {
                throw new NotFoundItemInPortalException(id.getID());
            }
            Order order = new Gson().fromJson(onlyJson, Order.class);
            orderCacheService.createOrder(order); // se achou manda pra cache
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
        removeClientOrderId(toDeleteOrder.getCID(), id.getID());
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
        clientOrders.add(orderId); // adiciona no que tinha
        pushClientOrders(clientId, clientOrders); // joga no ratis
        orderCacheService.setClientOrderId(orderId, clientOrders); // seta mesmo, não é add
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

        ArrayList<String> newClientOrders = new ArrayList<>(clientOrders.stream().filter(order -> !orderId.equals(order)).toList()); // remove no que tinha
        pushClientOrders(clientId, newClientOrders); // joga no ratis
        orderCacheService.setClientOrderId(clientId, newClientOrders); // seta na cache
    }
}
