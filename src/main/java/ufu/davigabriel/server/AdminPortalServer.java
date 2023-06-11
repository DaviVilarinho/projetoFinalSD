package ufu.davigabriel.server;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.stub.StreamObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.exceptions.PortalException;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.models.ProductNative;
import ufu.davigabriel.models.ReplyNative;
import ufu.davigabriel.services.ClientUpdaterMiddleware;
import ufu.davigabriel.services.ProductCacheService;

import javax.sound.sampled.Port;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class AdminPortalServer {
    public static int BASE_PORTAL_SERVER_PORT = 25506; // definindo porta base para servers
    private static Logger logger = LoggerFactory.getLogger(AdminPortalServer.class);
    private Server server;

    public static void main(String[] args) throws IOException, InterruptedException {
        int port = BASE_PORTAL_SERVER_PORT;

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
        private ProductCacheService productCacheService = ProductCacheService.getInstance();
        private ClientUpdaterMiddleware clientUpdaterMiddleware = ClientUpdaterMiddleware.getInstance();

        @Override
        public void createClient(Client request, StreamObserver<Reply> responseObserver) {
            logger.info("CRIAR CLIENTE " + request.toString());
            try {
                clientUpdaterMiddleware.createClient(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                logger.info("CLIENTE CRIADO");
            } catch (PortalException exception) {
                logger.error("REQUISICAO DE CLIENTE " + request + "NAO CRIADO.\n " + exception.getMessage() +
                                     "\n" + exception.getStackTrace().toString());
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void retrieveClient(ID request, StreamObserver<Client> responseObserver) {
            logger.info("BUSCAR CLIENTE " + request.toString());
            try {
                responseObserver.onNext(clientUpdaterMiddleware.getClient(request));
                logger.debug("CLIENTE RETORNADO COM SUCESSO");
            } catch (PortalException exception) {
                logger.info("NÃO FOI POSSÍVEL BUSCAR O CLIENTE " + request + " retornando nulo. " + exception.getMessage());
                exception.printStackTrace();
                responseObserver.onNext(ClientNative.generateEmptyClientNative().toClient());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void updateClient(Client request, StreamObserver<Reply> responseObserver) {
            logger.info("DAR UPDATE EM CLIENTE " + request);
            try {
                clientUpdaterMiddleware.updateClient(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                logger.info("UPDATE CONCLUÍDO COM SUCESSO");
            } catch (PortalException exception) {
                logger.info("NÃO FOI POSSÍVEL ATUALIZAR O CLIENTE " + request + exception.getMessage());
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void deleteClient(ID request, StreamObserver<Reply> responseObserver) {
            logger.info("DELETAR " + request);
            try {
                clientUpdaterMiddleware.deleteClient(request);
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                logger.info("DELETADO: " + request);
            } catch (PortalException exception) {
                logger.info("NÃO FOI POSSÍVEL BUSCAR O CLIENTE " + request + " retornando nulo. " + exception.getMessage());
                exception.printStackTrace();
                exception.replyError(responseObserver);
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void createProduct(Product request, StreamObserver<Reply> responseObserver) {
            try {
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                throw new DuplicatePortalItemException();
            } catch (DuplicatePortalItemException e) {
                e.replyError(responseObserver);
            } catch (Exception specificException) {
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.ERRO_PROTOCOLOS.getError())
                                                .setDescription(ReplyNative.ERRO_PROTOCOLOS.getDescription())
                                                .build());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void retrieveProduct(ID request, StreamObserver<Product> responseObserver) {
            try {
                responseObserver.onNext(productCacheService.retrieveProduct(request).toProduct());
                throw new NotFoundItemInPortalException();
            } catch (NotFoundItemInPortalException exception) {
                responseObserver.onNext(ProductNative.generateEmptyProductNative().toProduct());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void updateProduct(Product request, StreamObserver<Reply> responseObserver) {
            try {
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                throw new NotFoundItemInPortalException();
            } catch (NotFoundItemInPortalException e) {
                e.replyError(responseObserver);
            } catch (Exception specificException) {
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.ERRO_PROTOCOLOS.getError())
                                                .setDescription(ReplyNative.ERRO_PROTOCOLOS.getDescription())
                                                .build());
            } finally {
                responseObserver.onCompleted();
            }
        }

        @Override
        public void deleteProduct(ID request, StreamObserver<Reply> responseObserver) {
            try {
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.SUCESSO.getError())
                                                .setDescription(ReplyNative.SUCESSO.getDescription())
                                                .build());
                throw new NotFoundItemInPortalException();
            } catch (NotFoundItemInPortalException e) {
                e.replyError(responseObserver);
            } catch (Exception specificException) {
                responseObserver.onNext(Reply.newBuilder()
                                                .setError(ReplyNative.ERRO_PROTOCOLOS.getError())
                                                .setDescription(ReplyNative.ERRO_PROTOCOLOS.getDescription())
                                                .build());
            } finally {
                responseObserver.onCompleted();
            }
        }
    }
}
