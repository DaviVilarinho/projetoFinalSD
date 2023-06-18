package ufu.davigabriel.services;

import ufu.davigabriel.exceptions.*;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Product;

public interface IClientProxyDatabase {
    void createClient(Client client) throws DuplicatePortalItemException, RatisClientException, BadRequestException;
    void updateClient(Client client) throws NotFoundItemInPortalException, RatisClientException, BadRequestException, IllegalVersionPortalItemException;
    Client retrieveClient(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException;
    void deleteClient(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException;
}
