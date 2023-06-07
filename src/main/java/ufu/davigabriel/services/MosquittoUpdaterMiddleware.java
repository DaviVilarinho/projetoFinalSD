package ufu.davigabriel.services;

import lombok.Getter;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.util.Random;

/**
 * Um Middleware se fez necessario no Projeto na medida em que:
 * * E possivel escutar requests
 * * E possivel mudar
 * * E necessario atualizar multiplos servers
 *
 * O Middleware seria nosso proxy para a database. Toda *mudanca* (nao consulta) ao banco
 * de dados precisa passar por aqui porque se faz necessario espalhar as mudancas pelas instancias do server
 * e notificar mudancas recebidas via request.
 *
 * Por questao de arquitetura e configuracoes do mosquitto, este middleware conecta a uma instancia
 * Mosquitto e publica toda mudanca assim que recebida, nao realiza mudancas locais.
 * Paralelamente, existe uma thread subscrita aos topicos de interesse definidos nas classes que herdam esta
 * que forma o segundo elo do middleware que seria atualizar assim que recebida mensagens de mudanca
 */
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
        } catch (MqttException me) {
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
        } catch (MqttException e) {
            System.out.println("Nao foi possivel desconectar do broker... Conexao estagnada");
        }
    }
}
