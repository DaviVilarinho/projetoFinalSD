package ufu.davigabriel.services;

import ufu.davigabriel.exceptions.*;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Order;

import java.util.ArrayList;

public interface IOrderProxyDatabase {
    void createOrder(Order order) throws DuplicatePortalItemException, RatisClientException, BadRequestException, UnauthorizedUserException;
    void updateOrder(Order order) throws NotFoundItemInPortalException, RatisClientException, BadRequestException, UnauthorizedUserException, IllegalVersionPortalItemException;
    void deleteOrder(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException;
    Order retrieveOrder(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException;
    ArrayList<Order> retrieveClientOrders(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException;

    void addClientOrderId(String clientId, String orderId) throws RatisClientException;

    void removeClientOrderId(String clientId, String orderId) throws RatisClientException, BadRequestException;
}
