package ufu.davigabriel.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufu.davigabriel.exceptions.BadRequestException;
import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.exceptions.RatisClientException;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import java.nio.charset.Charset;

public class ClientUpdaterMiddleware extends UpdaterMiddleware implements IClientProxyDatabase {
    private static ClientUpdaterMiddleware instance;
    private static Logger logger = LoggerFactory.getLogger(ClientUpdaterMiddleware.class);
    private ClientCacheService clientCacheService = ClientCacheService.getInstance();

    public static ClientUpdaterMiddleware getInstance() {
        if (instance == null) {
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
    public void createClient(Client client) throws DuplicatePortalItemException, RatisClientException, BadRequestException {
        if (clientCacheService.hasClient(client)) {
            throw new DuplicatePortalItemException("Usuário já existe: " + client.toString());
        }
        try {
            getRatisClientFromID(client.getCID()).add(getClientStorePath(client),
                                                      ClientNative.fromClient(client).toJson());
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            logger.debug("Erro json inválido: " + client);
            throw new BadRequestException();
        }
        clientCacheService.createClient(client);
    }

    @Override
    public void updateClient(Client client) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        if (!clientCacheService.hasClient(client)) {
            retrieveClient(client); // TODO avaliar se está lançando o NOTFOUND
        }
        try {
            clientCacheService.updateClient(ClientNative.fromJson(getRatisClientFromID(client.getCID()).update(
                    getClientStorePath(client), client.toString()).getMessage().getContent().toString(
                    Charset.defaultCharset())).toClient());
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            logger.debug("Erro json inválido: " + client);
            throw new BadRequestException();
        }
    }

    public Client retrieveClient(Client client) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        return retrieveClient(ID.newBuilder().setID(client.getCID()).build());
    }

    @Override
    public Client retrieveClient(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        try {
            return clientCacheService.retrieveClient(id);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            logger.debug("ID não encontrado, tentando buscar no bd" + id.toString());
        }

        try {
            Client client = ClientNative.fromJson(getRatisClientFromID(id.getID()).get(
                    getStorePath(id.getID())).getMessage().getContent().toString(
                    Charset.defaultCharset())).toClient();
            clientCacheService.createClient(client);
            return client;
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
    public void deleteClient(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        if (!clientCacheService.hasClient(id.getID())) {
            retrieveClient(id);
        }
        try {
            clientCacheService.deleteClient(ID.newBuilder().setID(ClientNative.fromJson(
                    getRatisClientFromID(id.getID()).del(
                            getStorePath(id.getID().toString())).getMessage().getContent().toString(
                            Charset.defaultCharset())).getCID()).build());
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            logger.debug("Erro json inválido: " + id);
            throw new BadRequestException();
        }
    }
}