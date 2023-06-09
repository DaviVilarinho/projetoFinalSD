package ufu.davigabriel.services;

import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.exceptions.RatisClientException;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.distributedDatabase.RatisClient;

import java.nio.charset.Charset;

public class ClientUpdaterMiddleware extends UpdaterMiddleware implements IClientProxyDatabase {
    ClientCacheService clientCacheService = ClientCacheService.getInstance();

    private RatisClient getRatisClientFromClientCID(int cid) {
        return this.getRatisClients()[cid % 2];
    }

    private RatisClient getRatisClientFromClientCID(Client client) {
        return getRatisClientFromClientCID(Integer.parseInt(client.getCID()));
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
        getRatisClientFromClientCID(client).add(getClientStorePath(client),
                                                ClientNative.fromClient(client).toJson());
        clientCacheService.createClient(client);
    }

    @Override
    public void updateClient(Client client) throws NotFoundItemInPortalException, RatisClientException {
        if (!clientCacheService.hasClient(client)) {
            getClient(client); // TODO avaliar se está lançando o NOTFOUND
        }
        clientCacheService.updateClient(ClientNative.fromJson(getRatisClientFromClientCID(client).update(
                getClientStorePath(client), client.toString()).getMessage().getContent().toString(
                Charset.defaultCharset())).toClient()); // TODO avaliar get nulo
    }

    public Client getClient(Client client) throws NotFoundItemInPortalException, RatisClientException {
        return getClient(ID.newBuilder().setID(client.getCID()).build());
    }

    @Override
    public Client getClient(ID id) throws NotFoundItemInPortalException, RatisClientException {
        try {
            return clientCacheService.getClient(id);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            Client client = ClientNative.fromJson(getRatisClientFromClientCID(Integer.parseInt(id.getID())).get(
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
            getClient(id); // TODO avaliar se está lançando o NOTFOUND
        }
        clientCacheService.deleteClient(ClientNative.fromJson(
                getRatisClientFromClientCID(Integer.parseInt(id.getID())).del(
                        getStorePath(id.getID().toString())).getMessage().getContent().toString(
                        Charset.defaultCharset())).toClient().getCID()); // TODO avaliar get nulo
    }
}
