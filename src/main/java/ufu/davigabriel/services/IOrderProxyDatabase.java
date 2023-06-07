package ufu.davigabriel.services;

import org.eclipse.paho.client.mqttv3.MqttException;
import ufu.davigabriel.exceptions.BadRequestException;
import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.exceptions.UnauthorizedUserException;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Order;

/**
 * O Middleware e a Database fazem uso desta interface para redirecionar as atribuicoes de cada
 * Middleware: falar para todos que houve mudancas
 * Database: realizar mudanca
 */
public interface IOrderProxyDatabase {
    void createOrder(Order order) throws DuplicatePortalItemException, MqttException, UnauthorizedUserException, NotFoundItemInPortalException, BadRequestException;
    void updateOrder(Order order) throws NotFoundItemInPortalException, MqttException, UnauthorizedUserException, BadRequestException;
    void deleteOrder(ID id) throws NotFoundItemInPortalException, MqttException;
}
