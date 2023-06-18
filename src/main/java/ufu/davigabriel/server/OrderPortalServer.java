package ufu.davigabriel.server;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import ufu.davigabriel.exceptions.*;
import ufu.davigabriel.models.GlobalVarsService;
import ufu.davigabriel.models.OrderNative;
import ufu.davigabriel.models.ReplyNative;
import ufu.davigabriel.services.OrderUpdaterMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class OrderPortalServer {
    private static Logger logger = LoggerFactory.getLogger(AdminPortalServer.class);
    private Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = GlobalVarsService.ORDER_PORTAL_SERVER_BASE_PORT;
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
        private OrderUpdaterMiddleware orderUpdaterMiddleware = OrderUpdaterMiddleware.getInstance();

        @Override
        public void createOrder(Order request, StreamObserver<Reply> responseObserver) {
            System.out.println("CRIAR PEDIDO " + request.toString());
            try {
                orderUpdaterMiddleware.createOrder(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                System.out.println("PEDIDO CRIADO");
            } catch (PortalException exception) {
                logger.error("NÃO FOI POSSÍVEL CRIAR O PEDIDO " + request + "retornando nulo.\n " + exception.getMessage() +
                                     "\n" + exception.getStackTrace().toString());
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void retrieveOrder(ID request, StreamObserver<Order> responseObserver) {
            System.out.println("BUSCAR PEDIDO " + request.toString());
            try {
                responseObserver.onNext(orderUpdaterMiddleware.retrieveOrder(request));
                System.out.println("PEDIDO RETORNADO COM SUCESSO");
            } catch (PortalException exception) {
                System.out.println("NÃO FOI POSSÍVEL BUSCAR O PEDIDO " + request + " retornando nulo. " + exception.getMessage());
                exception.printStackTrace();
                responseObserver.onNext(OrderNative.generateEmptyOrderNative().toOrder());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void updateOrder(Order request, StreamObserver<Reply> responseObserver) {
            System.out.println("DAR UPDATE EM PEDIDO " + request);
            try {
                orderUpdaterMiddleware.updateOrder(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                System.out.println("UPDATE CONCLUÍDO COM SUCESSO");
            } catch (PortalException exception) {
                logger.error("NÃO FOI POSSÍVEL ATUALIZAR O PEDIDO " + request + exception.getMessage());
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void deleteOrder(ID request, StreamObserver<Reply> responseObserver) {
            System.out.println("DELETAR " + request);
            try {
                orderUpdaterMiddleware.deleteOrder(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                System.out.println("DELETADO: " + request);
            } catch (PortalException exception) {
                System.out.println("NÃO FOI POSSÍVEL DELETAR O PEDIDO " + request + " retornando nulo. " + exception.getMessage());
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void retrieveClientOrders(ID request, StreamObserver<Order> responseObserver) {
            try {
                orderUpdaterMiddleware.retrieveClientOrders(request).forEach(responseObserver::onNext);
                System.out.println("Listado " + request);
            } catch (PortalException exception) {
                System.out.println("Não foi possível listar " + request);
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }
    }
}