package ufu.davigabriel.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufu.davigabriel.exceptions.*;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import java.nio.charset.Charset;
import java.util.HashMap;

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
            retrieveClient(client);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            System.out.println("Não há usuário " + client.getCID() + ", prosseguindo com create");
        }
        if (clientCacheService.hasClient(client)) {
            throw new DuplicatePortalItemException(client.getCID());
        }
        try {
            if (!getRatisClientFromID(client.getCID()).add(getClientStorePath(client), new Gson().toJson(client)).isSuccess()) {
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
                    getClientStorePath(client), new Gson().toJson(client)).isSuccess()) {
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
            String onlyJson = queryClient.split(":", 2)[1];
            System.out.println("Banco encontrou cliente: " + queryClient);
            if ("null".equals(onlyJson)) {
                throw new NotFoundItemInPortalException(id.getID());
            }
            Client client = new Gson().fromJson(onlyJson, Client.class);
            clientCacheService.createClient(client);
            System.out.println("Retornando Cliente que encontrou no banco " + client.toString() + ", já criado na cache");
            return client;
        } catch (JsonSyntaxException | ArrayIndexOutOfBoundsException jsonSyntaxException) {
            System.out.println("Erro no parse do json, provavelmente get:null");
            jsonSyntaxException.printStackTrace();
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
        retrieveClient(id);
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
