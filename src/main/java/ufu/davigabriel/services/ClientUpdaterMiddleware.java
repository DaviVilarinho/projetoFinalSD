package ufu.davigabriel.services;

import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufu.davigabriel.exceptions.*;
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
        try {
            Client retrieveClient = retrieveClient(client);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            System.out.println("Não há usuário " + client.getCID() + ", prosseguindo com create");
        }
        try {
            if (!getRatisClientFromID(client.getCID()).add(getClientStorePath(client), ClientNative.fromClient(client).toJson()).isSuccess()) {
                throw new DuplicatePortalItemException();
            }
            clientCacheService.createClient(client);
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + client);
            throw new BadRequestException();
        }
    }

    @Override
    public void updateClient(Client client) throws NotFoundItemInPortalException, RatisClientException, BadRequestException, IllegalVersionPortalItemException {
        retrieveClient(client);
        clientCacheService.throwIfNotUpdatable(ClientNative.fromClient(client));
        try {
            if (!getRatisClientFromID(client.getCID()).update(
                    getClientStorePath(client), client.toString()).isSuccess()) {
                throw new NotFoundItemInPortalException();
            }
            clientCacheService.updateClient(client);
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + client);
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
            System.out.println("ID não encontrado, tentando buscar no bd " + id.toString());
        }

        try {
            String queryClient = getRatisClientFromID(id.getID()).get(getStorePath(id.getID())).getMessage().getContent().toString(Charset.defaultCharset());
            Client client = ClientNative.fromJson(queryClient).toClient();
            clientCacheService.createClient(client);
            return client;
        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new NotFoundItemInPortalException();
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + id);
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
            if (!getRatisClientFromID(id.getID()).del(getStorePath(id.getID())).isSuccess()) {
                throw new NotFoundItemInPortalException();
            }
            clientCacheService.deleteClient(id);
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + id);
            throw new BadRequestException();
        }
    }
}
