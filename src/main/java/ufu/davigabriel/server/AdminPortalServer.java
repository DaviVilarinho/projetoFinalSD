package ufu.davigabriel.server;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.MqttException;
import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.models.ProductNative;
import ufu.davigabriel.models.ReplyNative;
import ufu.davigabriel.services.AdminDatabaseService;
import ufu.davigabriel.services.MosquittoAdminUpdaterMiddleware;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AdminPortalServer {
    public static int BASE_PORTAL_SERVER_PORT = 25506; // definindo porta base para servers
    private Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = BASE_PORTAL_SERVER_PORT;

        try {
            if (args.length > 0) { // aceita dinamicamente portas somado ao valor base
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65536) throw new NumberFormatException("Porta com numero invalido");
            }
        } catch (NumberFormatException numberFormatException) {
            System.out.println("Se quiser conectar em alguma porta, por favor" +
                    " insira o argumento como uma string representando um int" +
                    " valido entre 1024 e 65535");
        } finally {
            System.out.println("Conectara em: " + port);
        }
        AdminPortalServer server = new AdminPortalServer();
        server.start(port);
        System.out.println("Admin portal running...");
        server.blockUntilShutdown();
    }

    private void start(int port) throws IOException {
        server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(new AdminPortalImpl())
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    AdminPortalServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("SERVER ENCERRADO");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination(); // GRPC roda em daemons...
        }
    }

    static public class AdminPortalImpl extends AdminPortalGrpc.AdminPortalImplBase {
        // singleton de db, necessario apenas em buscas porque demais mudancas devem passar pelo MIDDLEWARE
        private AdminDatabaseService adminDatabaseService = AdminDatabaseService.getInstance();

        // Singleton do Middleware GRPC. Atua como proxy, mudancas sao repassadas para ele, que publica e ao receber noticias muda
        private MosquittoAdminUpdaterMiddleware mosquittoAdminUpdaterMiddleware = MosquittoAdminUpdaterMiddleware.getInstance();


        /**
            As operacoes de escrita, delecao e update nao podem ser executadas localmente.
            Isso se deve ao fato de que o Middleware recebe mensagens inclusive de si mesmo.

            Entao por uma questao de consistencia, realiza essas operacoes publicando a mensagem recebida,
            todos ouvem e realizam a mudanca de acordo com a mensagem recebida.

            Existe ainda inconsistencia eventual se perder conexao, mas nesse caso o cluster deixou de ser
            um cluster e nao e o objetivo deste projeto.
         */
        @Override
        public void createClient(Client request, StreamObserver<Reply> responseObserver) {
            try {
                mosquittoAdminUpdaterMiddleware.createClient(request);
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.SUCESSO.getError())
                        .setDescription(ReplyNative.SUCESSO.getDescription())
                        .build());
            } catch (DuplicatePortalItemException exception) {
                exception.replyError(responseObserver);
            } catch (MqttException mqttException) {
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.ERRO_MQTT.getError())
                        .setDescription(ReplyNative.ERRO_MQTT.getDescription())
                        .build());
            } finally {
                responseObserver.onCompleted();
            }
        }

        /**
         * No caso de Retrieves, nao e necessario consultar o middleware. Um possivel caso
         * que seria necessario seria se houvesse metodos para atualizar todos os servidores.
         * Mas como nao existe, basta pegar a cache local
         */
        @Override
        public void retrieveClient(ID request, StreamObserver<Client> responseObserver) {
            try {
                responseObserver.onNext(adminDatabaseService.retrieveClient(request).toClient());
            } catch (NotFoundItemInPortalException exception) {
                responseObserver.onNext(ClientNative.generateEmptyClientNative().toClient());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void updateClient(Client request, StreamObserver<Reply> responseObserver) {
            try {
                mosquittoAdminUpdaterMiddleware.updateClient(request);
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.SUCESSO.getError())
                        .setDescription(ReplyNative.SUCESSO.getDescription())
                        .build());
            } catch (MqttException mqttException) {
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.ERRO_MQTT.getError())
                        .setDescription(ReplyNative.ERRO_MQTT.getDescription())
                        .build());
            } catch (NotFoundItemInPortalException e) {
                e.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void deleteClient(ID request, StreamObserver<Reply> responseObserver) {
            try {
                mosquittoAdminUpdaterMiddleware.deleteClient(request);
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.SUCESSO.getError())
                        .setDescription(ReplyNative.SUCESSO.getDescription())
                        .build());
            } catch (NotFoundItemInPortalException notFoundItemInDatabaseException) {
                notFoundItemInDatabaseException.replyError(responseObserver);
            } catch (MqttException mqttException) {
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.ERRO_MQTT.getError())
                        .setDescription(ReplyNative.ERRO_MQTT.getDescription())
                        .build());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void createProduct(Product request, StreamObserver<Reply> responseObserver) {
            try {
                mosquittoAdminUpdaterMiddleware.createProduct(request);
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.SUCESSO.getError())
                        .setDescription(ReplyNative.SUCESSO.getDescription())
                        .build());
            } catch (MqttException mqttException) {
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.ERRO_MQTT.getError())
                        .setDescription(ReplyNative.ERRO_MQTT.getDescription())
                        .build());
            } catch (DuplicatePortalItemException e) {
                e.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        /**
         * mesmo caso relatado anteriormente
         */
        @Override
        public void retrieveProduct(ID request, StreamObserver<Product> responseObserver) {
            try {
                responseObserver.onNext(adminDatabaseService.retrieveProduct(request).toProduct());
            } catch (NotFoundItemInPortalException exception) {
                responseObserver.onNext(ProductNative.generateEmptyProductNative().toProduct());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void updateProduct(Product request, StreamObserver<Reply> responseObserver) {
            try {
                mosquittoAdminUpdaterMiddleware.updateProduct(request);
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.SUCESSO.getError())
                        .setDescription(ReplyNative.SUCESSO.getDescription())
                        .build());
            } catch (MqttException mqttException) {
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.ERRO_MQTT.getError())
                        .setDescription(ReplyNative.ERRO_MQTT.getDescription())
                        .build());
            } catch (NotFoundItemInPortalException e) {
                e.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void deleteProduct(ID request, StreamObserver<Reply> responseObserver) {
            try {
                mosquittoAdminUpdaterMiddleware.deleteProduct(request);
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.SUCESSO.getError())
                        .setDescription(ReplyNative.SUCESSO.getDescription())
                        .build());
            } catch (MqttException mqttException) {
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.ERRO_MQTT.getError())
                        .setDescription(ReplyNative.ERRO_MQTT.getDescription())
                        .build());
            } catch (NotFoundItemInPortalException e) {
                e.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }
    }
}
