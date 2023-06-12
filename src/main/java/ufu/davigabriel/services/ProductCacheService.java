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
public class ProductCacheService implements IProductProxyDatabase {
    private static ProductCacheService instance;
    /*
    O esquema de dados nos Hash Maps abaixo (productsMap e clientsMap) ocorre
     da seguinte maneira:
    String (ID) -> String (JSON) representando, respectivamente, um product e
     um client.
    */
    private HashMap<String, String> productsMap;

    private ProductCacheService() {
        if (instance == null) {
            productsMap = new HashMap<>();
        }
    }

    public static ProductCacheService getInstance() {
        if (instance == null) {
            instance = new ProductCacheService();
        }
        return instance;
    }

    public void printAllProdcuts() {
        productsMap.forEach((s, productNative) -> System.out.println(productNative));
    }

    public void createProduct(Product product) throws DuplicatePortalItemException {
        createProduct(ProductNative.fromProduct(product));
    }

    private void createProduct(ProductNative productNative) throws DuplicatePortalItemException {
        if (hasProduct(productNative.getPID()))
            throw new DuplicatePortalItemException();

        productsMap.putIfAbsent(productNative.getPID(), productNative.toJson());
    }

    public Product retrieveProduct(ID id) throws NotFoundItemInPortalException {
        return retrieveProduct(id.getID()).toProduct();
    }

    private ProductNative retrieveProduct(String id) throws NotFoundItemInPortalException {
        if (!hasProduct(id)) throw new NotFoundItemInPortalException();
        return ProductNative.fromJson(productsMap.get(id));
    }

    public void updateProduct(Product product) throws NotFoundItemInPortalException {
        updateProduct(ProductNative.fromProduct(product));
    }

    private void updateProduct(ProductNative productNative) throws NotFoundItemInPortalException {
        if (!hasProduct(productNative.getPID())) throw new NotFoundItemInPortalException();
        productsMap.put(productNative.getPID(), productNative.toJson());
    }

    public void deleteProduct(ID id) throws NotFoundItemInPortalException {
        deleteProduct(id.getID());
    }

    private void deleteProduct(String id) throws NotFoundItemInPortalException {
        if (!hasProduct(id)) throw new NotFoundItemInPortalException();
        productsMap.remove(id);
    }

    public boolean hasProduct(String id) { return productsMap.containsKey(id); }

    public boolean hasProduct(Product product) { return hasProduct(product.getPID()); }
}
