package ufu.davigabriel.services;

import org.eclipse.paho.client.mqttv3.MqttException;
import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Product;

/**
 * O Middleware e a Database fazem uso desta interface para redirecionar as atribuicoes de cada
 * Middleware: falar para todos que houve mudancas
 * Database: realizar mudanca
 */
public interface IAdminProxyDatabase {
    void createClient(Client client) throws DuplicatePortalItemException, MqttException;
    void updateClient(Client client) throws NotFoundItemInPortalException, MqttException;
    void deleteClient(ID id) throws NotFoundItemInPortalException, MqttException;

    void createProduct(Product product) throws DuplicatePortalItemException, MqttException;
    void updateProduct(Product Product) throws NotFoundItemInPortalException, MqttException;
    void deleteProduct(ID id) throws NotFoundItemInPortalException, MqttException;
}
