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


/**
 * ClientUpdaterMiddleware
 * No passado era um Mosquitto, aqui é o que gerencia mudanças que passam pra CacheService
 * Nesse caso de Client ele é o homem do meio que verifica se a versão que possui (se possui) é válida
 * e espalha a mudança para o Ratis de acordo com a paridade do CID
 */
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
        // encontra se existe em qualquer jeito
        try {
            retrieveClient(client);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            System.out.println("Não há usuário " + client.getCID() + ", prosseguindo com create");
        }
        // se a cache encontrou
        if (clientCacheService.hasClient(client)) {
            throw new DuplicatePortalItemException(client.getCID());
        }
        try {
            // tenta mandar pro Ratis
            if (!getRatisClientFromID(client.getCID()).add(getClientStorePath(client), new Gson().toJson(client)).isSuccess()) {
                throw new DuplicatePortalItemException();
            }
            // tenta updatar na cache depois
            clientCacheService.createClient(client);
        } catch (IllegalStateException | NumberFormatException | NullPointerException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + client);
            throw new BadRequestException();
        }
    }

    @Override
    public void updateClient(Client client) throws NotFoundItemInPortalException, RatisClientException, BadRequestException, IllegalVersionPortalItemException {
        // pega/atualiza/throwa se não  tem
        retrieveClient(client);
        clientCacheService.throwIfNotUpdatable(ClientNative.fromClient(client)); // verifica se pode atualizar
        try {
            // tenta enviar pro ratis
            if (!getRatisClientFromID(client.getCID()).update(
                    getClientStorePath(client), new Gson().toJson(client)).isSuccess()) {
                throw new NotFoundItemInPortalException();
            }
            // atualiza na cache
            clientCacheService.updateClient(client);
        } catch (IllegalStateException | NumberFormatException | NullPointerException illegalStateException) {
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
        // busca na cache
        try {
            return clientCacheService.retrieveClient(id);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            System.out.println("ID não encontrado, tentando buscar no bd " + id.toString());
        }
        // se não tiver ou desatualizado
        try {
            // pega do BD primeiro
            String queryClient = getRatisClientFromID(id.getID()).get(getStorePath(id.getID())).getMessage().getContent().toString(Charset.defaultCharset());
            System.out.println("Banco encontrou cliente: " + queryClient);
            String onlyJson = queryClient.split(":", 2)[1];
            if ("null".equals(onlyJson)) {
                throw new NotFoundItemInPortalException(id.getID());
            }
            // se não tinha já throw de Não encontrado ou inválido
            Client client = new Gson().fromJson(onlyJson, Client.class);
            clientCacheService.createClient(client); // cria na cache
            System.out.println("Retornando Cliente que encontrou no banco " + client.toString() + ", já criado na cache");
            return client; // retorna
        } catch (JsonSyntaxException | ArrayIndexOutOfBoundsException jsonSyntaxException) {
            System.out.println("Erro no parse do json, provavelmente get:null");
            jsonSyntaxException.printStackTrace();
            throw new NotFoundItemInPortalException();
        } catch (IllegalStateException | NumberFormatException | NullPointerException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + id);
            throw new BadRequestException();
        } catch (DuplicatePortalItemException e) {
            e.printStackTrace();
            throw new RatisClientException("Erro de sincronizacao interna com a database " + id.getID()); // nunca deu, só aconteceria se não tinha e ao dar create tem
        }
    }

    @Override
    public void deleteClient(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        retrieveClient(id); // verifica se tem, se não tem estoura a exceção de acordo
        try {
            if (!getRatisClientFromID(id.getID()).del(getStorePath(id.getID())).isSuccess()) {
                throw new NotFoundItemInPortalException();
            }
            clientCacheService.deleteClient(id); // se der, enviar pra cache que deletou
        } catch (IllegalStateException | NumberFormatException | NullPointerException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + id);
            throw new BadRequestException();
        }
    }
}
