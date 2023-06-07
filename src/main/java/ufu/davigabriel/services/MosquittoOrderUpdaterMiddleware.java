package ufu.davigabriel.services;

import com.google.gson.Gson;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import ufu.davigabriel.Main;
import ufu.davigabriel.exceptions.BadRequestException;
import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.exceptions.UnauthorizedUserException;
import ufu.davigabriel.models.OrderItemNative;
import ufu.davigabriel.models.OrderNative;
import ufu.davigabriel.models.ProductNative;
import ufu.davigabriel.server.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;

/*
Diferentemente do 'MosquitoAdminUpdaterMiddleware', esta classe nao pode atualizar na resposta de uma subscricao, no 'AdminPortalServer',
via requests, a quantidade de produtos. Isso se deve ao fato de que existem multiplas instancias e todas executarao o callback para
mudar o estado local. No entanto, esta seria uma mudanca no estado global.
 */
public class MosquittoOrderUpdaterMiddleware extends MosquittoUpdaterMiddleware implements IOrderProxyDatabase {
    private static MosquittoOrderUpdaterMiddleware instance;
    private AdminPortalGrpc.AdminPortalBlockingStub connectionBlockingStub;
    final private OrderDatabaseService orderDatabaseService = OrderDatabaseService.getInstance();

    public MosquittoOrderUpdaterMiddleware() {
        super();

        String CONNECTION_SERVER = String.format("%s:%d", "localhost", AdminPortalServer.BASE_PORTAL_SERVER_PORT + new Random().nextInt(Main.PORTAL_SERVERS));
        ManagedChannel connectionChannel = Grpc.newChannelBuilder(CONNECTION_SERVER, InsecureChannelCredentials.create()).build();
        this.connectionBlockingStub = AdminPortalGrpc.newBlockingStub(connectionChannel);
    }

    public void authenticateClient(String CID) throws UnauthorizedUserException {
        Client client = connectionBlockingStub.retrieveClient(ID.newBuilder().setID(CID).build());

        if("0".equals(client.getCID())) throw new UnauthorizedUserException();
    }

    @Override
    public String[] getInterestedTopics() {
        Object[] objectTopicsToSubscribe = Arrays.stream(MosquittoTopics.values())
                .map(MosquittoTopics::name)
                .filter(name -> name.startsWith("ORDER"))
                .toArray();

        return Arrays.copyOf(objectTopicsToSubscribe, objectTopicsToSubscribe.length, String[].class);
    }

    public static MosquittoOrderUpdaterMiddleware getInstance() {
        if (instance == null)
            instance = new MosquittoOrderUpdaterMiddleware();

        return instance;
    }

    public void publishOrderChange(Order order, MosquittoTopics mosquittoTopics) throws MqttException {
        super.getMqttClient().publish(mosquittoTopics.name(), new MqttMessage(new Gson().toJson(order).getBytes()));
    }

    public void publishOrderDeletion(ID id) throws MqttException {
        super.getMqttClient().publish(MosquittoTopics.ORDER_DELETION_TOPIC.name(), new MqttMessage(id.toByteArray()));
    }

    public void validateProductInOrder(String id, int quantityRequest) throws NotFoundItemInPortalException, BadRequestException {
        ProductNative productNative = ProductNative.fromProduct(connectionBlockingStub.retrieveProduct(ID.newBuilder().setID(id).build()));
        if("0".equals(productNative.getPID()))
            throw new NotFoundItemInPortalException();

        if(productNative.getQuantity() <= 0 || productNative.getQuantity() < quantityRequest)
            throw new BadRequestException("Quantidade de produto invalida.");
    }

    public void validateOrderProducts(ArrayList<OrderItemNative> products) throws NotFoundItemInPortalException, BadRequestException {
        if (products.isEmpty())
            throw new BadRequestException("Produto vazio.");
        for (OrderItemNative product : products) {
            validateProductInOrder(product.getPID(), product.getQuantity());
        }
    }

    public OrderNative validateOrder(Order order) throws NotFoundItemInPortalException, BadRequestException {
        OrderNative orderNative = OrderNative.fromOrder(order);
        validateOrderProducts(orderNative.getProducts());
        return orderNative;
    }

    public void throwIfDuplicatedOrder(String id) throws DuplicatePortalItemException {
        if(orderDatabaseService.hasOrder(id))
            throw new DuplicatePortalItemException();
    }

    @Override
    public void createOrder(Order order) throws DuplicatePortalItemException, MqttException, UnauthorizedUserException, NotFoundItemInPortalException, BadRequestException {
        authenticateClient(order.getCID());
        throwIfDuplicatedOrder(order.getOID());
        OrderNative orderNative = validateOrder(order);
        for (OrderItemNative product : orderNative.getProducts()) {
            if (product.getQuantity() == 0)
                throw new BadRequestException("Produto com quantidade 0.");
        }
        publishOrderChange(order, MosquittoTopics.ORDER_CREATION_TOPIC);
        orderNative.getProducts().forEach(item -> {
            increaseGlobalProductQuantity(connectionBlockingStub, item.getPID(), -item.getQuantity());
        });
    }

    /*
    Uma vez que, ao atualizar um pedido, é possível que haja
    diminuição/aumento de quantidade de produtos com mesmo PID, foi
    implementado um método, por meio de um hash map auxiliar, que contabiliza
     todas as operações em um único valor e, após isso, realiza apenas uma
     correção de quantidade por produto.

     Ex.: OrderAntiga, Produto X com QTD = 5 | OrderAtualizada, Produto X com
      QTD = 9.
      Cálculo: +5 -9 -> -4 ==> Valor que será somado à quantidade do Produto X.
     */
    @Override
    public void updateOrder(Order order) throws NotFoundItemInPortalException, MqttException, UnauthorizedUserException, BadRequestException {
        authenticateClient(order.getCID());
        if (!orderDatabaseService.hasOrder(order.getOID()))
            throw new NotFoundItemInPortalException();

        OrderNative oldOrderNative = orderDatabaseService.retrieveOrder(order.getOID());
        OrderNative newOrderNative = validateOrder(order);
        newOrderNative.getProducts().removeIf(product -> product.getQuantity() == 0);
        publishOrderChange(order, MosquittoTopics.ORDER_UPDATE_TOPIC);

        HashMap<String, Integer> auxiliarHashMapForProductQuantityRestoration = new HashMap<String, Integer>();

        oldOrderNative.getProducts().forEach(oldItem -> {
            auxiliarHashMapForProductQuantityRestoration.put(oldItem.getPID(),
                    auxiliarHashMapForProductQuantityRestoration.getOrDefault(oldItem.getPID(), 0) + oldItem.getQuantity());
        });

        newOrderNative.getProducts().forEach(newItem -> {
            auxiliarHashMapForProductQuantityRestoration.put(newItem.getPID(),
                    auxiliarHashMapForProductQuantityRestoration.getOrDefault(newItem.getPID(), 0) - newItem.getQuantity());
        });

        auxiliarHashMapForProductQuantityRestoration.forEach((id, value) -> {
            increaseGlobalProductQuantity(connectionBlockingStub, id, value);
        });
    }

    @Override
    public void deleteOrder(ID id) throws NotFoundItemInPortalException, MqttException {
        OrderNative orderNative = orderDatabaseService.retrieveOrder(id);
        if (!orderDatabaseService.hasOrder(id.getID()))
            throw new NotFoundItemInPortalException();
        publishOrderDeletion(id);
        orderNative.getProducts().forEach(item -> {
            increaseGlobalProductQuantity(connectionBlockingStub, item.getPID(), item.getQuantity());
        });
    }

    private void increaseGlobalProductQuantity(AdminPortalGrpc.AdminPortalBlockingStub blockingStub, String productId, int variation){
        ProductNative productToBeAdjusted = ProductNative.fromProduct(blockingStub.retrieveProduct(ID.newBuilder().setID(productId).build()));
        productToBeAdjusted.setQuantity(productToBeAdjusted.getQuantity()+variation);
        blockingStub.updateProduct(productToBeAdjusted.toProduct());
    }
}
