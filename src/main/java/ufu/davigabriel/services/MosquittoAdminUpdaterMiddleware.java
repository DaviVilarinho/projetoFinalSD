package ufu.davigabriel.services;

import com.google.gson.Gson;
import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Product;

import java.util.Arrays;


public class MosquittoAdminUpdaterMiddleware extends MosquittoUpdaterMiddleware implements IClientProxyDatabase {
    private static MosquittoAdminUpdaterMiddleware instance;
    final private ClientCacheService clientCacheService = ClientCacheService.getInstance();
    
    public MosquittoAdminUpdaterMiddleware(){
        super();
    }
    
    public String[] getInterestedTopics()  {
        Object[] objectTopicsToSubscribe = Arrays.stream(MosquittoTopics.values())
                .map(MosquittoTopics::name)
                .filter(name -> name.startsWith("CLIENT") || name.startsWith("PRODUCT"))
                .toArray();
        
        return Arrays.copyOf(objectTopicsToSubscribe, objectTopicsToSubscribe.length, String[].class);
    }

    public static MosquittoAdminUpdaterMiddleware getInstance() {
        if (instance == null)
            instance = new MosquittoAdminUpdaterMiddleware();

        return instance;
    }
    private void publishClientChange(Client client, MosquittoTopics mosquittoTopics) throws Exception {
        super.getMqttClient().publish(mosquittoTopics.name(), new MqttMessage(new Gson().toJson(client).getBytes()));
    }

    private void publishClientDeletion(ID id) throws Exception {
        super.getMqttClient().publish(MosquittoTopics.CLIENT_DELETION_TOPIC.name(), new MqttMessage(id.toByteArray()));
    }

    private void publishProductChange(Product product, MosquittoTopics mosquittoTopics) throws Exception {
        super.getMqttClient().publish(mosquittoTopics.name(), new MqttMessage(new Gson().toJson(product).getBytes()));
    }

    private void publishProductDeletion(ID id) throws Exception {
        super.getMqttClient().publish(MosquittoTopics.PRODUCT_DELETION_TOPIC.name(), new MqttMessage(id.toByteArray()));
    }
    @Override
    public void createClient(Client client) throws DuplicatePortalItemException, Exception {
        if (clientCacheService.hasClient(client.getCID()))
            throw new DuplicatePortalItemException();
        publishClientChange(client, MosquittoTopics.CLIENT_CREATION_TOPIC);
    }

    @Override
    public void updateClient(Client client) throws NotFoundItemInPortalException, Exception {
        if (!clientCacheService.hasClient(client.getCID()))
            throw new NotFoundItemInPortalException();
        publishClientChange(client, MosquittoTopics.CLIENT_UPDATE_TOPIC);
    }

    @Override
    public void deleteClient(ID id) throws NotFoundItemInPortalException, Exception {
        if (!clientCacheService.hasClient(id.getID()))
            throw new NotFoundItemInPortalException();
        publishClientDeletion(id);
    }

    @Override
    public void createProduct(Product product) throws DuplicatePortalItemException, Exception {
        if (clientCacheService.hasProduct(product.getPID()))
            throw new DuplicatePortalItemException();
        publishProductChange(product, MosquittoTopics.PRODUCT_CREATION_TOPIC);
    }

    @Override
    public void updateProduct(Product product) throws NotFoundItemInPortalException, Exception {
        if (!clientCacheService.hasProduct(product.getPID()))
            throw new NotFoundItemInPortalException();
        publishProductChange(product, MosquittoTopics.PRODUCT_UPDATE_TOPIC);
    }

    @Override
    public void deleteProduct(ID id) throws NotFoundItemInPortalException, Exception {
        if (!clientCacheService.hasProduct(id.getID()))
            throw new NotFoundItemInPortalException();
        publishProductDeletion(id);
    }
}
