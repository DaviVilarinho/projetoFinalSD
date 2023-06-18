package ufu.davigabriel.server;

import com.google.gson.Gson;
import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.exceptions.PortalException;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.models.GlobalVarsService;
import ufu.davigabriel.models.ProductNative;
import ufu.davigabriel.models.ReplyNative;
import ufu.davigabriel.services.ClientUpdaterMiddleware;
import ufu.davigabriel.services.ProductUpdaterMiddleware;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AdminPortalServer {
    private static Logger logger = LoggerFactory.getLogger(AdminPortalServer.class);
    private Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = GlobalVarsService.ADMIN_PORTAL_SERVER_BASE_PORT;

        try {
            if (args.length > 0) { // aceita dinamicamente portas somado ao valor base
                port = Integer.parseInt(args[0]);
                if (port < 1024 || port > 65536) {
                    throw new NumberFormatException("Porta com numero invalido");
                }
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
        private ClientUpdaterMiddleware clientUpdaterMiddleware = ClientUpdaterMiddleware.getInstance();
        private ProductUpdaterMiddleware productUpdaterMiddleware = ProductUpdaterMiddleware.getInstance();

        @Override
        public void createClient(Client request, StreamObserver<Reply> responseObserver) {
            System.out.println("CRIAR CLIENTE " + new Gson().toJson(request.toString()));
            try {
                clientUpdaterMiddleware.createClient(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                System.out.println("CLIENTE CRIADO");
            } catch (PortalException exception) {
                logger.error("NÃO FOI POSSÍVEL CRIAR O CLIENTE " + request + "retornando nulo.\n " + exception.getMessage() +
                                     "\n" + exception.getStackTrace().toString());
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void retrieveClient(ID request, StreamObserver<Client> responseObserver) {
            System.out.println("BUSCAR CLIENTE " + request.toString());
            try {
                responseObserver.onNext(clientUpdaterMiddleware.retrieveClient(request));
                System.out.println("CLIENTE RETORNADO COM SUCESSO");
            } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
                responseObserver.onNext(ClientNative.generateEmptyClientNative().toClient());
                System.out.println("CLIENTE NÃO EXISTE, RETORNADO VAZIO");
            } catch (PortalException exception) {
                System.out.println("NÃO FOI POSSÍVEL BUSCAR O CLIENTE " + request + " retornando nulo. " + exception.getMessage());
                exception.printStackTrace();
                responseObserver.onNext(ClientNative.generateEmptyClientNative().toClient());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void updateClient(Client request, StreamObserver<Reply> responseObserver) {
            System.out.println("DAR UPDATE EM CLIENTE " + request);
            try {
                clientUpdaterMiddleware.updateClient(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                System.out.println("UPDATE CONCLUÍDO COM SUCESSO");
            } catch (PortalException exception) {
                System.out.println("NÃO FOI POSSÍVEL ATUALIZAR O CLIENTE " + request + exception.getMessage());
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void deleteClient(ID request, StreamObserver<Reply> responseObserver) {
            System.out.println("DELETAR " + request);
            try {
                clientUpdaterMiddleware.deleteClient(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                System.out.println("DELETADO: " + request);
            } catch (PortalException exception) {
                System.out.println("NÃO FOI POSSÍVEL DELETAR O CLIENTE " + request + " retornando nulo. " + exception.getMessage());
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void createProduct(Product request, StreamObserver<Reply> responseObserver) {
            System.out.println("CRIAR PRODUTO " + request.toString());
            try {
                productUpdaterMiddleware.createProduct(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                System.out.println("PRODUTO CRIADO");
            } catch (PortalException exception) {
                logger.error("NÃO FOI POSSÍVEL CRIAR O PRODUTO " + request + "retornando nulo.\n " + exception.getMessage() +
                                     "\n" + exception.getStackTrace().toString());
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void retrieveProduct(ID request, StreamObserver<Product> responseObserver) {
            System.out.println("BUSCAR PRODUTO " + request.toString());
            try {
                responseObserver.onNext(productUpdaterMiddleware.retrieveProduct(request));
                System.out.println("PRODUTO RETORNADO COM SUCESSO");
            } catch (PortalException exception) {
                System.out.println("NÃO FOI POSSÍVEL BUSCAR O PRODUTO " + request + " retornando nulo. " + exception.getMessage());
                exception.printStackTrace();
                responseObserver.onNext(ProductNative.generateEmptyProductNative().toProduct());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void updateProduct(Product request, StreamObserver<Reply> responseObserver) {
            System.out.println("DAR UPDATE EM PRODUTO " + request);
            try {
                productUpdaterMiddleware.updateProduct(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                System.out.println("UPDATE CONCLUÍDO COM SUCESSO");
            } catch (PortalException exception) {
                System.out.println("NÃO FOI POSSÍVEL ATUALIZAR O PRODUTO " + request + exception.getMessage());
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void deleteProduct(ID request, StreamObserver<Reply> responseObserver) {
            System.out.println("DELETAR " + request);
            try {
                productUpdaterMiddleware.deleteProduct(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                System.out.println("DELETADO: " + request);
            } catch (PortalException exception) {
                System.out.println("NÃO FOI POSSÍVEL DELETAR O PRODUTO " + request + " retornando nulo. " + exception.getMessage());
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }
    }
}
