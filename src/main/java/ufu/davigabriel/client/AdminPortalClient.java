package ufu.davigabriel.client;

import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.models.GlobalVarsService;
import ufu.davigabriel.models.ProductNative;
import ufu.davigabriel.models.ReplyNative;
import ufu.davigabriel.server.*;

import javax.swing.text.html.Option;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

public class AdminPortalClient {
    public static String HOST = "127.0.0.1";
    public static int SERVER_PORT = GlobalVarsService.ADMIN_PORTAL_SERVER_BASE_PORT + new Random().nextInt(GlobalVarsService.PORTAL_SERVERS);
    public static String TARGET_SERVER = String.format("%s:%d", HOST, SERVER_PORT);
    private static AdminPortalGrpc.AdminPortalBlockingStub blockingStub;
    private static final Scanner scanner = new Scanner(System.in);
    private static final HashMap<String, String> myHashes = new HashMap<>();

    public static void main(String[] args) throws InterruptedException {
        System.out.println("----------------------------------");
        System.out.println("Bem vindo ao Portal Administrativo");
        System.out.println("----------------------------------");

        try {
            if (args.length > 0) {
                SERVER_PORT = Integer.parseInt(args[0]);
                if (SERVER_PORT < 1024 || SERVER_PORT > 65536) throw new NumberFormatException("Porta com numero invalido");
                TARGET_SERVER = String.format("%s:%d", HOST,
                        SERVER_PORT);
            }
        } catch (NumberFormatException numberFormatException) {
            System.out.println("Se quiser conectar em alguma porta, por favor" +
                    " insira o argumento como uma string representando um int" +
                    " valido entre 1024 e 65535");
        } finally {
            System.out.println("Conectara em: " + TARGET_SERVER);
        }

        ManagedChannel channel = Grpc.newChannelBuilder(TARGET_SERVER, InsecureChannelCredentials.create()).build();

        try {
            AdminPortalClient.blockingStub = AdminPortalGrpc.newBlockingStub(channel);

            System.out.println("Conectado com server " + TARGET_SERVER);

            AdminPortalOption adminPortalOption = AdminPortalOption.NOOP;
            while (!AdminPortalOption.SAIR.equals(adminPortalOption)) {
                System.out.println("^^--__--^^");
                System.out.println("Versões do que você já lidou: " + myHashes.keySet().toString());
                System.out.println("Opcoes:");
                Arrays.stream(AdminPortalOption.values()).forEach(System.out::println);

                System.out.print("Escolha: ");
                try {
                    adminPortalOption = AdminPortalOption.valueOf(scanner.nextLine());
                } catch (NullPointerException |
                         IllegalArgumentException exception) {
                    System.out.println("Por favor, escolha outra opcao.");
                    adminPortalOption = AdminPortalOption.NOOP;
                }

                switch (adminPortalOption) {
                    case NOOP -> System.out.println("Nada a ser feito.");
                    case CRIAR_CLIENTE -> {
                        System.out.print("Escreva o nome do cliente: ");
                        String name = scanner.nextLine();
                        System.out.print("Escreva o zipCode do cliente: ");
                        String zipCode = scanner.nextLine();

                        String cid = geraId(name);
                        System.out.println("Escolhendo ID: " + cid);
                        ReplyNative response = createClient(blockingStub, ClientNative.builder().CID(cid).name(name).zipCode(zipCode).build());
                        if (response.getError() != 0)
                            System.out.println("ERRO: " + response.getDescription());
                        else System.out.println("CLIENTE INSERIDO");
                    }
                    case BUSCAR_CLIENTE -> {
                        System.out.print("Escreva o ID do cliente: ");
                        Optional<ClientNative> foundClient = retrieveClient(blockingStub, scanner.nextLine());
                        foundClient.ifPresentOrElse(client -> {
                            System.out.println("CLIENTE ENCONTRADO");
                            System.out.println(client);
                        }, () -> System.out.println("CLIENTE NAO ENCONTRADO"));
                    }
                    case MUDAR_CLIENTE -> {
                        System.out.print("Escreva o ID do cliente a mudar: ");
                        String cidAMudar = scanner.nextLine();

                        System.out.print("Escreva o novo nome do cliente: ");
                        String name = scanner.nextLine();
                        System.out.print("Escreva o novo zipCode do cliente: ");
                        String zipCode = scanner.nextLine();

                        ReplyNative response = updateClient(blockingStub, ClientNative.builder().CID(cidAMudar).name(name).zipCode(zipCode).build());
                        if (response.getError() != 0)
                            System.out.println("ERRO: " + response.getDescription());
                        else System.out.println("CLIENTE ALTERADO");
                    }
                    case REMOVER_CLIENTE -> {
                        System.out.print("Escreva o ID do cliente: ");

                        ReplyNative response = removeClient(blockingStub, scanner.nextLine());
                        if (response.getError() != 0)
                            System.out.println("ERRO: " + response.getDescription());
                        else System.out.println("CLIENTE REMOVIDO");
                    }
                    case CRIAR_PRODUTO -> {
                        System.out.print("Escreva o nome do novo produto: ");
                        String name = scanner.nextLine();
                        System.out.print("Escreva uma descricao do produto: ");
                        String description = scanner.nextLine();
                        try {
                            System.out.print("Escreva o preco do produto: ");
                            double price = Double.parseDouble(scanner.nextLine());
                            System.out.print("Escreva a quantidade do produto: ");
                            int quantity = Integer.parseInt(scanner.nextLine());

                            String productId = geraId(name);
                            System.out.println("ID a ser usado nele: " + productId);

                            ReplyNative response = createProduct(blockingStub, ProductNative.builder().PID(productId).name(name).description(description).price(price).quantity(quantity).build());
                            if (response.getError() != 0)
                                System.out.println("ERRO: " + response.getDescription());
                            else System.out.println("PRODUTO INSERIDO");
                        } catch (NullPointerException |
                                 NumberFormatException formatException) {
                            System.out.println("Este produto e invalido e nao sera inserido");
                        }
                    }
                    case BUSCAR_PRODUTO -> {
                        System.out.print("Escreva o ID do produto: ");
                        Optional<ProductNative> foundProduct = retrieveProduct(blockingStub, scanner.nextLine());
                        foundProduct.ifPresentOrElse(productNative -> {
                            System.out.println("PRODUTO ENCONTRADO");
                            System.out.println(productNative);
                        }, () -> System.out.println("PRODUTO NAO ENCONTRADO"));
                    }
                    case MUDAR_PRODUTO -> {
                        System.out.print("Escreva o ID do produto a ser alterado: ");
                        String targetProductId = scanner.nextLine();
                        System.out.print("Escreva o nome do novo produto: ");
                        String name = scanner.nextLine();
                        System.out.print("Escreva uma descricao do produto: ");
                        String description = scanner.nextLine();
                        try {
                            System.out.print("Escreva o preco do produto: ");
                            double price = Double.parseDouble(scanner.nextLine());
                            System.out.print("Escreva a quantidade do " +
                                    "produto: ");
                            int quantity = Integer.parseInt(scanner.nextLine());

                            ReplyNative response = updateProduct(blockingStub, ProductNative.builder().PID(targetProductId).name(name).description(description).price(price).quantity(quantity).build());
                            if (response.getError() != 0)
                                System.out.println("ERRO: " + response.getDescription());
                            else System.out.println("PRODUTO ATUALIZADO");
                        } catch (NullPointerException |
                                 NumberFormatException formatException) {
                            System.out.println("Este produto e invalido e nao sera atualizado");
                        }
                    }
                    case REMOVER_PRODUTO -> {
                        System.out.print("Escreva o ID do produto: ");

                        ReplyNative response = removeProduct(blockingStub, scanner.nextLine());
                        if (response.getError() != 0)
                            System.out.println("ERRO: " + response.getDescription());
                        else System.out.println("PRODUTO REMOVIDO");
                    }
                    default -> {
                        System.out.println("Encerrando o portal administrativo.");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException exception) {
                            exception.printStackTrace();
                        }
                        adminPortalOption = AdminPortalOption.SAIR;
                    }
                }
            }
        } finally {
            scanner.close();
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    static public String geraId(String nome) {
        CRC32 crc32 = new CRC32();
        crc32.update(nome.getBytes());
        return Integer.valueOf(Math.abs((int) crc32.getValue())).toString();
    }

    static private ReplyNative createClient(AdminPortalGrpc.AdminPortalBlockingStub blockingStub, ClientNative clientNative) {
        myHashes.put(clientNative.getCID(), clientNative.getHash());
        return ReplyNative.fromReply(blockingStub.createClient(clientNative.toClient()));
    }

    static private Optional<ClientNative> retrieveClient(AdminPortalGrpc.AdminPortalBlockingStub blockingStub, String clientId) {
        ClientNative client = ClientNative.fromClient(blockingStub.retrieveClient(ID.newBuilder().setID(clientId).build()));
        Optional<ClientNative> optClient = Optional.empty();
        if (!"0".equals(client.getCID())) {
            optClient = Optional.of(client);
            myHashes.put(clientId, client.getHash());
        }
        return optClient;
    }

    static private ReplyNative updateClient(AdminPortalGrpc.AdminPortalBlockingStub blockingStub, ClientNative clientNative) {
        if (!myHashes.containsKey(clientNative.getCID())) {
            Optional<ClientNative> optionalClientNative = retrieveClient(blockingStub, clientNative.getCID());
            optionalClientNative.ifPresent((oldClient) -> {
                System.out.println("Como você ainda não tinha se relacionado com o CID "
                                                                            + clientNative.getCID()
                                                                            + ", a atualização será baseada neste cliente " + oldClient.toJson());
                if (!"0".equals(oldClient.getCID())) {myHashes.put(oldClient.getCID(), oldClient.getHash());}
            });
        }
        clientNative.setUpdatedVersionHash(myHashes.getOrDefault(clientNative.getCID(), ""));
        ReplyNative replyNative = ReplyNative.fromReply(blockingStub.updateClient(clientNative.toClient()));
        myHashes.put(clientNative.getCID(), clientNative.getHash());
        return replyNative;
    }

    static private ReplyNative removeClient(AdminPortalGrpc.AdminPortalBlockingStub blockingStub, String clientId) {
        myHashes.remove(clientId);
        return ReplyNative.fromReply(blockingStub.deleteClient(ID.newBuilder().setID(clientId).build()));
    }

    static private ReplyNative createProduct(AdminPortalGrpc.AdminPortalBlockingStub blockingStub, ProductNative productNative) {
        myHashes.put(productNative.getPID(), productNative.getHash());
        return ReplyNative.fromReply(blockingStub.createProduct(productNative.toProduct()));
    }

    static private Optional<ProductNative> retrieveProduct(AdminPortalGrpc.AdminPortalBlockingStub blockingStub, String productId) {
        Product product = blockingStub.retrieveProduct(ID.newBuilder().setID(productId).build());
        Optional<ProductNative> optionalProduct = Optional.empty();
        if (!"0".equals(product.getPID())) {
            ProductNative productNative = ProductNative.fromProduct(product);
            optionalProduct = Optional.of(productNative);
            myHashes.put(productId, productNative.getHash());
        }
        return optionalProduct;
    }

    static private ReplyNative updateProduct(AdminPortalGrpc.AdminPortalBlockingStub blockingStub, ProductNative productNative) {
        if (!myHashes.containsKey(productNative.getPID())) {
            Optional<ProductNative> optionalProductNative = retrieveProduct(blockingStub, productNative.getPID());
            optionalProductNative.ifPresent((oldProduct) -> {
                System.out.println("Como você ainda não tinha se relacionado com o PID "
                                           + productNative.getPID()
                                           + ", a atualização será baseada neste produto " + oldProduct.toJson());
                if (!"0".equals(oldProduct.getPID())) {myHashes.put(oldProduct.getPID(), oldProduct.getHash());}
            });
        }
        productNative.setUpdatedVersionHash(myHashes.getOrDefault(productNative.getPID(), ""));
        ReplyNative replyNative = ReplyNative.fromReply(blockingStub.updateProduct(productNative.toProduct()));
        myHashes.put(productNative.getPID(), productNative.getHash());
        return replyNative;
    }

    static private ReplyNative removeProduct(AdminPortalGrpc.AdminPortalBlockingStub blockingStub, String productId) {
        myHashes.remove(productId);
        return ReplyNative.fromReply(blockingStub.deleteProduct(ID.newBuilder().setID(productId).build()));
    }
}
