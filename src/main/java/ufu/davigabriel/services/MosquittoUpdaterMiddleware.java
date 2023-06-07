package ufu.davigabriel.services;

import lombok.Getter;

import java.util.Random;

@Getter
public abstract class MosquittoUpdaterMiddleware {
    final private boolean SHOULD_CONNECT_ONLINE = false;
    final private String RANDOM_ID = Integer.valueOf(new Random().nextInt(100000000)).toString();
    final private String CLIENT_ID = SHOULD_CONNECT_ONLINE ? "publisher-davi-vilarinho-gabriel-amaral-gbc074" : RANDOM_ID;
    final private MemoryPersistence PERSISTENCE = new MemoryPersistence();
    private final int QOS = 2;
    private MqttClient mqttClient;

    MosquittoUpdaterMiddleware() {
        try {
            String BROKER = SHOULD_CONNECT_ONLINE ? "tcp://broker.hivemq.com:1883" : "tcp://localhost:1883";
            this.mqttClient = new MqttClient(BROKER, CLIENT_ID, PERSISTENCE);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setUserName(CLIENT_ID);
            connOpts.setCleanSession(true);
            connOpts.setAutomaticReconnect(true);
            System.out.println("Inicializando conexao com broker MQTT");
            this.mqttClient.setCallback(new MosquittoTopicCallback());
            this.mqttClient.connect();
            System.out.println("Conectado com sucesso");
            this.mqttClient.subscribe(getInterestedTopics());
            System.out.println("Subscrito...");
        } catch (Exception me) {
            System.out.println("Nao foi possivel inicializar o client MQTT, encerrando");
            System.out.println("reason: " + me.getReasonCode());
            System.out.println("msg: " + me.getMessage());
            System.out.println("loc: " + me.getLocalizedMessage());
            System.out.println("cause: " + me.getCause());
            System.out.println("exception: " + me);
            me.printStackTrace();
            System.exit(-1);
        } catch (EnumConstantNotPresentException enumConstantNotPresentException) {
            System.out.println("Contexto inexistente...");
            System.exit(-1);
        }
    }

    public abstract String[] getInterestedTopics();

    public void disconnect() {
        System.out.println("Desconectando...");
        try {
            this.mqttClient.disconnect();
            System.out.println("Desconectado com sucesso");
        } catch (Exception e) {
            System.out.println("Nao foi possivel desconectar do broker... Conexao estagnada");
        }
    }
}
