package ufu.davigabriel.services;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

/**
 * Esta simples classe apenas define o comportamento padrao ao
 * receber mensagens nos topicos subscritos
 */
public class MosquittoTopicCallback implements MqttCallback {
    @Override
    public void connectionLost(Throwable cause) {
        cause.printStackTrace();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        System.out.println("Mensagem recebida de: " + topic + message.toString());
        // escolher a ENUM de valor igual ao topico subscrito e rodar o callback associado ao topico
        MosquittoTopics.valueOf(topic).getIMqttMessageListener().accept(topic, message);
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        System.out.println("Entregue com sucesso..." + token);
    }
}
