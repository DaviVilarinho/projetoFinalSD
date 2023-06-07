package ufu.davigabriel.services;

import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.models.ProductNative;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Product;

import java.util.HashMap;

/**
 * Aqui sao realizadas consultas e mudancas diretamente nas tabelas de clientes e produtos
 *
 * Sao operacoes simples de CRUD que nao compensam explicacoes
 *
 * Em geral evitamos inconsistencia, mesmo eventual, entao mesmo que
 * quem chame verifique tambem a possibilidade ou nao de uma operacao,
 * a database de Admin nao permitira operacoes produtos ou clientes duplicados ou
 * inexistentes.
 */
public class AdminDatabaseService implements IAdminProxyDatabase {
    private static AdminDatabaseService instance;
    /*
    O esquema de dados nos Hash Maps abaixo (productsMap e clientsMap) ocorre
     da seguinte maneira:
    String (ID) -> String (JSON) representando, respectivamente, um product e
     um client.
    */
    private HashMap<String, String> productsMap;
    private HashMap<String, String> clientsMap;

    private AdminDatabaseService() {
        if (instance == null) {
            productsMap = new HashMap<>();
            clientsMap = new HashMap<>();
        }
    }

    public static AdminDatabaseService getInstance() {
        if (instance == null) {
            instance = new AdminDatabaseService();
        }
        return instance;
    }

    public void listAllClients() {
        clientsMap.forEach((s, client) -> System.out.println(client));
    }

    public void listAllProdcuts() {
        productsMap.forEach((s, productNative) -> System.out.println(productNative));
    }

    public void createClient(Client client) throws DuplicatePortalItemException {
        createClient(ClientNative.fromClient(client));
    }

    public void createClient(ClientNative clientNative) throws DuplicatePortalItemException {
        if (hasClient(clientNative.getCID()))
            throw new DuplicatePortalItemException();

        clientsMap.putIfAbsent(clientNative.getCID(), clientNative.toJson());
    }

    public ClientNative retrieveClient(ID id) throws NotFoundItemInPortalException {
        return retrieveClient(id.getID());
    }

    public ClientNative retrieveClient(String id) throws NotFoundItemInPortalException {
        if (!hasClient(id)) throw new NotFoundItemInPortalException();
        return ClientNative.fromJson(clientsMap.get(id));
    }

    public void updateClient(Client client) throws NotFoundItemInPortalException {
        updateClient(ClientNative.fromClient(client));
    }

    public void updateClient(ClientNative clientNative) throws NotFoundItemInPortalException {
        if (!hasClient(clientNative.getCID())) throw new NotFoundItemInPortalException();
        clientsMap.put(clientNative.getCID(), clientNative.toJson());
    }

    public void deleteClient(ID id) throws NotFoundItemInPortalException {
        if (!hasClient(id.getID())) throw new NotFoundItemInPortalException();
        deleteClient(id.getID());
    }

    public void deleteClient(String id) throws NotFoundItemInPortalException {
        if (!hasClient(id)) throw new NotFoundItemInPortalException();
        clientsMap.remove(id);
    }

    public boolean hasClient(String id) { return clientsMap.containsKey(id); }

    public void createProduct(Product product) throws DuplicatePortalItemException {
        createProduct(ProductNative.fromProduct(product));
    }

    public void createProduct(ProductNative productNative) throws DuplicatePortalItemException {
        if (hasProduct(productNative.getPID()))
            throw new DuplicatePortalItemException();

        productsMap.putIfAbsent(productNative.getPID(), productNative.toJson());
    }

    public ProductNative retrieveProduct(ID id) throws NotFoundItemInPortalException {
        return retrieveProduct(id.getID());
    }

    public ProductNative retrieveProduct(String id) throws NotFoundItemInPortalException {
        if (!hasProduct(id)) throw new NotFoundItemInPortalException();
        return ProductNative.fromJson(productsMap.get(id));
    }

    public void updateProduct(Product product) throws NotFoundItemInPortalException {
        updateProduct(ProductNative.fromProduct(product));
    }

    public void updateProduct(ProductNative productNative) throws NotFoundItemInPortalException {
        if (!hasProduct(productNative.getPID())) throw new NotFoundItemInPortalException();
        productsMap.put(productNative.getPID(), productNative.toJson());
    }

    public void deleteProduct(ID id) throws NotFoundItemInPortalException {
        deleteProduct(id.getID());
    }

    public void deleteProduct(String id) throws NotFoundItemInPortalException {
        if (!hasProduct(id)) throw new NotFoundItemInPortalException();
        productsMap.remove(id);
    }

    public boolean hasProduct(String id) { return productsMap.containsKey(id); }
}
