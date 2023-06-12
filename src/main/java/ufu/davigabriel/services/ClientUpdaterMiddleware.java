package ufu.davigabriel.services;

import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.exceptions.RatisClientException;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;

import java.nio.charset.Charset;

public class ClientUpdaterMiddleware extends UpdaterMiddleware implements IClientProxyDatabase {
    private static ClientUpdaterMiddleware instance;
    private ClientCacheService clientCacheService = ClientCacheService.getInstance();

    public static ClientUpdaterMiddleware getInstance() {
        if (instance == null){
            instance = new ClientUpdaterMiddleware();
        }
        return instance;
    }

    public String getClientStorePath(Client client) {
        return super.getStorePath(client.getCID());
    }

    @Override
    public String getSelfSavePath() {
        return "clients/";
    }

    @Override
    public void createClient(Client client) throws DuplicatePortalItemException, RatisClientException {
        if (clientCacheService.hasClient(client)) {
            throw new DuplicatePortalItemException("Usuário já existe: " + client.toString());
        }
        getRatisClientFromID(client.getCID()).add(getClientStorePath(client),
                                                ClientNative.fromClient(client).toJson());
        clientCacheService.createClient(client);
    }

    @Override
    public void updateClient(Client client) throws NotFoundItemInPortalException, RatisClientException {
        if (!clientCacheService.hasClient(client)) {
            retrieveClient(client); // TODO avaliar se está lançando o NOTFOUND
        }
        clientCacheService.updateClient(ClientNative.fromJson(getRatisClientFromID(client.getCID()).update(
                getClientStorePath(client), client.toString()).getMessage().getContent().toString(
                Charset.defaultCharset())).toClient()); // TODO avaliar get nulo
    }

    public Client retrieveClient(Client client) throws NotFoundItemInPortalException, RatisClientException {
        return retrieveClient(ID.newBuilder().setID(client.getCID()).build());
    }

    @Override
    public Client retrieveClient(ID id) throws NotFoundItemInPortalException, RatisClientException {
        try {
            return clientCacheService.retrieveClient(id);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            Client client = ClientNative.fromJson(getRatisClientFromID(id.getID()).get(
                    getStorePath(id.getID())).getMessage().getContent().toString(
                    Charset.defaultCharset())).toClient(); // TODO avaliar get nulo
            try {
                clientCacheService.createClient(client);
            } catch (DuplicatePortalItemException e) {
                throw new RatisClientException("Erro de sincronizacao interna com a database " + id.getID());
            }

            return client;
        }
    }

    @Override
    public void deleteClient(ID id) throws NotFoundItemInPortalException, RatisClientException {
        if (!clientCacheService.hasClient(id.getID())) {
            retrieveClient(id); // TODO avaliar se está lançando o NOTFOUND
        }
        clientCacheService.deleteClient(ID.newBuilder().setID(ClientNative.fromJson(
                getRatisClientFromID(id.getID()).del(
                        getStorePath(id.getID().toString())).getMessage().getContent().toString(
                        Charset.defaultCharset())).getCID()).build()); // TODO avaliar get nulo
    }
}
