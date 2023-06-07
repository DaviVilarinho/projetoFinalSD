package ufu.davigabriel.server;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import org.eclipse.paho.client.mqttv3.MqttException;
import ufu.davigabriel.exceptions.*;
import ufu.davigabriel.models.OrderNative;
import ufu.davigabriel.models.ReplyNative;
import ufu.davigabriel.services.MosquittoOrderUpdaterMiddleware;
import ufu.davigabriel.services.OrderDatabaseService;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OrderPortalServer {
    private Server server;
    public static int BASE_PORTAL_SERVER_PORT = 60552;

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
        final OrderPortalServer server = new OrderPortalServer();
        server.start(port);
        System.out.println("Order Portal running...");
        server.blockUntilShutdown();
    }

    private void start(int port) throws IOException {
        /* The port on which the server should run */
        server = Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create())
                .addService(new OrderPortalServer.OrderPortalImpl())
                .build()
                .start();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    OrderPortalServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    private void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    /**
     * Await termination on the main thread since the grpc library uses daemon threads.
     */
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    static public class OrderPortalImpl extends OrderPortalGrpc.OrderPortalImplBase {

        private final OrderDatabaseService orderDatabaseService = OrderDatabaseService.getInstance();
        private final MosquittoOrderUpdaterMiddleware mosquittoOrderUpdaterMiddleware = MosquittoOrderUpdaterMiddleware.getInstance();

        @Override
        public void createOrder(Order request, StreamObserver<Reply> responseObserver) {
            try {
                mosquittoOrderUpdaterMiddleware.createOrder(request);
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.SUCESSO.getError())
                        .setDescription(ReplyNative.SUCESSO.getDescription())
                        .build());
            } catch (PortalException exception) {
                exception.replyError(responseObserver);
            } catch (MqttException e) {
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.ERRO_MQTT.getError())
                        .setDescription(ReplyNative.ERRO_MQTT.getDescription())
                        .build());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void retrieveOrder(ID request, StreamObserver<Order> responseObserver) {
            try {
                responseObserver.onNext(orderDatabaseService.retrieveOrder(request).toOrder());
            } catch (NotFoundItemInPortalException exception) {
                responseObserver.onNext(OrderNative.generateEmptyOrderNative().toOrder());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void updateOrder(Order request, StreamObserver<Reply> responseObserver) {
            try {
                mosquittoOrderUpdaterMiddleware.updateOrder(request);
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.SUCESSO.getError())
                        .setDescription(ReplyNative.SUCESSO.getDescription())
                        .build());
            } catch (PortalException exception) {
                exception.replyError(responseObserver);
            } catch (MqttException e) {
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.ERRO_MQTT.getError())
                        .setDescription(ReplyNative.ERRO_MQTT.getDescription())
                        .build());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void deleteOrder(ID request, StreamObserver<Reply> responseObserver) {
            try {
                mosquittoOrderUpdaterMiddleware.deleteOrder(request);
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.SUCESSO.getError())
                        .setDescription(ReplyNative.SUCESSO.getDescription())
                        .build());
            } catch (NotFoundItemInPortalException exception) {
                exception.replyError(responseObserver);
            } catch (MqttException e) {
                responseObserver.onNext(Reply.newBuilder()
                        .setError(ReplyNative.ERRO_MQTT.getError())
                        .setDescription(ReplyNative.ERRO_MQTT.getDescription())
                        .build());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void retrieveClientOrders(ID request, StreamObserver<Order> responseObserver) {
            try {
                mosquittoOrderUpdaterMiddleware.authenticateClient(request.getID());
                orderDatabaseService.retrieveClientOrders(request).forEach((order) -> {
                    responseObserver.onNext(order.toOrder());
                });
            } catch (PortalException e) {
                e.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }
    }

}
