package ufu.davigabriel.services;

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
public interface IClientProxyDatabase {
    void createClient(Client client) throws DuplicatePortalItemException, RatisClientException;
    void updateClient(Client client) throws NotFoundItemInPortalException, RatisClientException;

    Client getClient(ID id) throws NotFoundItemInPortalException, RatisClientException;
    void deleteClient(ID id) throws NotFoundItemInPortalException, RatisClientException;
}
