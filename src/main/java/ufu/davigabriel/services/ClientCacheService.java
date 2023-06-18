package ufu.davigabriel.services;

import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.IllegalVersionPortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import java.util.HashMap;

/**
 * O cache tem função de guardar um mapa do que ele é e outro com hashes de cada id que ver e seu TTL
 */
public class ClientCacheService extends BaseCacheService implements IClientProxyDatabase {
    private static ClientCacheService instance;
    /*
    O esquema de dados nos Hash Maps abaixo (productsMap e clientsMap) ocorre
     da seguinte maneira:
    String (ID) -> String (JSON) representando, respectivamente, um product e
     um client.
    */
    private HashMap<String, String> clientsMap;

    private ClientCacheService() {
        if (instance == null) {
            clientsMap = new HashMap<>();
        }
    }

    public static ClientCacheService getInstance() {
        if (instance == null) {
            instance = new ClientCacheService();
        }
        return instance;
    }

    public void printAllClients() {
        clientsMap.forEach((s, client) -> System.out.println(client));
    }

    public void createClient(Client client) throws DuplicatePortalItemException {
        createClient(ClientNative.fromClient(client));
    }

    private void createClient(ClientNative clientNative) throws DuplicatePortalItemException {
        if (hasClient(clientNative.getCID()))
            throw new DuplicatePortalItemException();
        this.addToCache(clientNative);
        clientsMap.put(clientNative.getCID(), clientNative.toJson());
    }


    public Client retrieveClient(ID id) throws NotFoundItemInPortalException {
        return retrieveClient(id.getID()).toClient();
    }

    private ClientNative retrieveClient(String id) throws NotFoundItemInPortalException {
        this.throwNotFoundItemIfOldOrNotFoundHash(id);
        if (!hasClient(id)) throw new NotFoundItemInPortalException();

        return ClientNative.fromJson(clientsMap.get(id));
    }

    public void updateClient(Client client) throws NotFoundItemInPortalException, IllegalVersionPortalItemException {
        updateClient(ClientNative.fromClient(client));
    }

    private void updateClient(ClientNative clientNative) throws NotFoundItemInPortalException, IllegalVersionPortalItemException {
        throwIfNotUpdatable(clientNative);
        if (!hasClient(clientNative.getCID())) throw new NotFoundItemInPortalException();
        clientsMap.put(clientNative.getCID(), clientNative.toJson());
        addToCache(clientNative);
    }

    public void deleteClient(ID id) throws NotFoundItemInPortalException {
        deleteClient(id.getID());
    }

    private void deleteClient(String id) throws NotFoundItemInPortalException {
        throwNotFoundItemIfOldOrNotFoundHash(id);
        if (!hasClient(id)) throw new NotFoundItemInPortalException();
        clientsMap.remove(id);
        removeFromCache(id);
    }

    public boolean hasClient(String id) {
        return clientsMap.containsKey(id) && !this.isCacheOldOrMissing(id);
    }
    public boolean hasClient(Client client) {
        return hasClient(client.getCID());
    }
}
