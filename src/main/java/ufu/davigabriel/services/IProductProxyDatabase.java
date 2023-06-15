package ufu.davigabriel.services;

import ufu.davigabriel.exceptions.BadRequestException;
import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.exceptions.RatisClientException;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Product;

/**
 * O Middleware e a Database fazem uso desta interface para redirecionar as atribuicoes de cada
 * Middleware: falar para todos que houve mudancas
 * Database: realizar mudanca
 */
public interface IProductProxyDatabase {
    void createProduct(Product product) throws DuplicatePortalItemException, RatisClientException, BadRequestException;
    void updateProduct(Product Product) throws NotFoundItemInPortalException, RatisClientException, BadRequestException;
    void deleteProduct(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException;
    Product retrieveProduct(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException;
}
