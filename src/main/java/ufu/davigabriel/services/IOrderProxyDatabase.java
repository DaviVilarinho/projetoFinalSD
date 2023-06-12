package ufu.davigabriel.services;

import ufu.davigabriel.exceptions.*;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Order;

import java.util.ArrayList;

/**
 * O Middleware e a Database fazem uso desta interface para redirecionar as atribuicoes de cada
 * Middleware: falar para todos que houve mudancas
 * Database: realizar mudanca
 */
public interface IOrderProxyDatabase {
    void createOrder(Order order) throws DuplicatePortalItemException, RatisClientException;
    void updateOrder(Order order) throws NotFoundItemInPortalException, RatisClientException;
    void deleteOrder(ID id) throws NotFoundItemInPortalException, RatisClientException;
    Order retrieveOrder(ID id) throws NotFoundItemInPortalException, RatisClientException;
    ArrayList<Order> retrieveClientOrders(ID id) throws NotFoundItemInPortalException, RatisClientException;
}
