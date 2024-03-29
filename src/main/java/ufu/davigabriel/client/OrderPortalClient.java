package ufu.davigabriel.client;

import io.grpc.Channel;
import io.grpc.Grpc;
import io.grpc.InsecureChannelCredentials;
import io.grpc.ManagedChannel;
import ufu.davigabriel.Main;
import ufu.davigabriel.models.*;
import ufu.davigabriel.server.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class OrderPortalClient {
    private static final String HOST = "127.0.0.1";
    private static int SERVER_PORT = GlobalVarsService.ORDER_PORTAL_SERVER_BASE_PORT + new Random().nextInt(GlobalVarsService.PORTAL_SERVERS);
    public static String TARGET_SERVER = String.format("%s:%d", HOST, SERVER_PORT);
    private static final Scanner scanner = new Scanner(System.in);
    private final OrderPortalGrpc.OrderPortalBlockingStub blockingStub;
    private static final HashMap<String, String> myHashes = new HashMap<>();

    public OrderPortalClient(Channel channel) {
        this.blockingStub = OrderPortalGrpc.newBlockingStub(channel);
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("----------------------------------");
        System.out.println("Bem vindo ao Portal de Pedidos");
        System.out.println("----------------------------------");

        int adminPortalServerPort = GlobalVarsService.getInstance().getRandomAdminPortalPort();

        try {
            if (args.length > 0) {
                SERVER_PORT = Integer.parseInt(args[0]);
                if (SERVER_PORT < 1024 || SERVER_PORT > 65536) throw new NumberFormatException("Porta com numero invalido");
                TARGET_SERVER = String.format("%s:%d", HOST,
                        SERVER_PORT);
            }
            if (args.length > 1) {
                adminPortalServerPort = Integer.parseInt(args[1]);
                if (adminPortalServerPort < 1024 || adminPortalServerPort > 65536) throw new NumberFormatException("Porta com numero invalido");
                adminPortalServerPort = GlobalVarsService.getInstance().getRandomAdminPortalPort();
            }
        } catch (NumberFormatException numberFormatException) {
            System.out.println("Se quiser conectar em alguma porta, por favor" +
                    " insira o argumento como uma string representando um int" +
                    " valido entre 1024 e 65535");
        } finally {
            System.out.println("Conectara em: " + TARGET_SERVER);
        }


        ManagedChannel channel = Grpc.newChannelBuilder(TARGET_SERVER, InsecureChannelCredentials.create()).build();

        String CONNECTION_SERVER = String.format("%s:%d", "127.0.0.1", adminPortalServerPort);
        ManagedChannel connectionChannel = Grpc.newChannelBuilder(CONNECTION_SERVER, InsecureChannelCredentials.create()).build();
        AdminPortalGrpc.AdminPortalBlockingStub adminPortalBlockingStub = AdminPortalGrpc.newBlockingStub(connectionChannel);

        try {
            OrderPortalClient orderPortalClient = new OrderPortalClient(channel);
            System.out.println("Conectado com server " + TARGET_SERVER);

            OrderPortalOption orderPortalOption = OrderPortalOption.NOOP;
            String loggedClientId = orderPortalClient.login(adminPortalBlockingStub, scanner);
            if (loggedClientId == null) {
                System.out.println("Tentativas esgotadas!");
                orderPortalOption = OrderPortalOption.SAIR;
            }

            while (!OrderPortalOption.SAIR.equals(orderPortalOption)) {
                System.out.println("^^--__");
                System.out.println("Ordens com versões que já lidou: " + myHashes.keySet());
                System.out.println("Opcoes:");
                Arrays.stream(OrderPortalOption.values()).forEach(System.out::println);

                System.out.print("Escolha: ");
                try {
                    orderPortalOption = OrderPortalOption.valueOf(scanner.nextLine());
                } catch (NullPointerException |
                         IllegalArgumentException exception) {
                    System.out.println("Por favor, escolha outra opcao.");
                    orderPortalOption = OrderPortalOption.NOOP;
                }

                switch (orderPortalOption) {
                    case NOOP -> System.out.println("Nada a ser feito.");
                    case CRIAR_PEDIDO -> {
                        String orderId = AdminPortalClient.geraId(UUID.randomUUID().toString());
                        System.out.print("Escreva a ID desejada para o pedido" +
                                                 " (Enter para " + orderId + "): ");
                        String inputOrderId = scanner.nextLine();
                        try {
                            inputOrderId = "".equals(inputOrderId.strip().trim()) ?
                                    orderId : inputOrderId;
                            inputOrderId = String.valueOf(Integer.parseInt(inputOrderId));
                            orderId = inputOrderId;
                        } catch (NumberFormatException numberFormatException) {
                            System.out.println(inputOrderId + " inválido... utilizando " + orderId);
                        }
                        ArrayList<OrderItemNative> addedProducts = new ArrayList<>();
                        String option;
                        addedProducts.add(orderPortalClient.addProductToOrder()); //1° produto
                        do {
                            System.out.print("Escreva se deseja adicionar novo produto ao pedido (y/n): ");
                            option = scanner.nextLine().strip().trim().toLowerCase();
                            if ("y".equals(option))
                                addedProducts.add(orderPortalClient.addProductToOrder());
                        } while (!"n".equals(option));

                        if (addedProducts.size() == 0) {
                            System.out.println("Sem produtos, sem update...");
                            break;
                        }

                        ReplyNative response = createOrder(orderPortalClient.blockingStub, OrderNative.builder().OID(orderId).CID(loggedClientId).products(addedProducts).build());
                        if (response.getError() != 0)
                            System.out.println("ERRO: " + response.getDescription());
                        else System.out.println("PEDIDO INSERIDO");
                    }
                    case BUSCAR_PEDIDO -> {
                        System.out.print("Escreva o ID do pedido: ");
                        Optional<OrderNative> foundOrder = retrieveOrder(orderPortalClient.blockingStub, scanner.nextLine());
                        foundOrder.ifPresentOrElse(orderNative -> {
                            System.out.println("PEDIDO ENCONTRADO");
                            double totalPrice = 0;
                            for (OrderItemNative item : orderNative.getProducts()) {
                                System.out.println(item);
                                totalPrice += item.getPrice();
                            }
                            System.out.println("Preco final do pedido: " + totalPrice);
                        }, () -> System.out.println("PEDIDO NAO ENCONTRADO"));
                    }
                    case MUDAR_PEDIDO -> {
                        System.out.print("Escreva a ID do pedido a mudar: ");
                        String oidAMudar = scanner.nextLine();

                        ArrayList<OrderItemNative> addedProducts = new ArrayList<>();
                        String option;
                        do {
                            System.out.print("Escreva se deseja adicionar novo produto ao pedido (y/n): ");
                            option = scanner.nextLine().strip().trim().toLowerCase();
                            if ("y".equals(option))
                                addedProducts.add(orderPortalClient.addProductToOrder());
                        } while (!"n".equals(option));
                        if (addedProducts.size() == 0) {
                            System.out.println("Sem produtos, sem update...");
                            break;
                        }

                        ReplyNative response = updateOrder(orderPortalClient.blockingStub, OrderNative.builder().OID(oidAMudar).CID(loggedClientId).products(addedProducts).build());
                        if (response.getError() != 0)
                            System.out.println("ERRO: " + response.getDescription());
                        else System.out.println("PEDIDO ATUALIZADO");
                    }
                    case REMOVER_PEDIDO -> {
                        System.out.print("Escreva o ID do pedido a ser removido: ");
                        ReplyNative response = removeOrder(orderPortalClient.blockingStub, scanner.nextLine());
                        if (response.getError() != 0)
                            System.out.println("ERRO: " + response.getDescription());
                        else System.out.println("PEDIDO REMOVIDO");
                    }
                    case MEUS_PEDIDOS -> {
                        Set<String> clientOrders = retrieveClientOrders(orderPortalClient.blockingStub, loggedClientId);
                        System.out.println("PEDIDOS ASSOCIADOS AO CLIENTE:");
                        clientOrders.forEach(orders -> {
                            System.out.println(orders);
                        });
                        System.out.println("------todos-pedidos-enumerados" +
                                "------");
                    }
                    default -> {
                        System.out.println("Encerrando o Portal de Pedidos.");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException exception) {
                            exception.printStackTrace();
                        }
                        orderPortalOption = OrderPortalOption.SAIR;
                    }
                }
            }
        } finally {
            connectionChannel.shutdownNow();
            channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    static private ReplyNative createOrder(OrderPortalGrpc.OrderPortalBlockingStub blockingStub, OrderNative orderNative) {
        myHashes.put(orderNative.getOID(), orderNative.getHash());
        return ReplyNative.fromReply(blockingStub.createOrder(orderNative.toOrder()));
    }

    static private Optional<OrderNative> retrieveOrder(OrderPortalGrpc.OrderPortalBlockingStub blockingStub, String orderId) {
        Order order = blockingStub.retrieveOrder(ID.newBuilder().setID(orderId).build());
        Optional<OrderNative> optOrder = Optional.empty();
        if (!"0".equals(order.getOID())) {
            OrderNative orderNative = OrderNative.fromOrder(order);
            optOrder = Optional.of(orderNative);
            myHashes.put(orderId, orderNative.getHash());
        }
        return optOrder;
    }

    static private ReplyNative removeOrder(OrderPortalGrpc.OrderPortalBlockingStub blockingStub, String orderId) {
        myHashes.remove(orderId);
        return ReplyNative.fromReply(blockingStub.deleteOrder(ID.newBuilder().setID(orderId).build()));
    }

    static private Set<String> retrieveClientOrders(OrderPortalGrpc.OrderPortalBlockingStub blockingStub, String clientId) {
        Set<String> clientOrders = new HashSet<>();
        blockingStub.retrieveClientOrders(ID.newBuilder().setID(clientId).build())
                .forEachRemaining(clientOrder -> {
                    if(clientOrder != null){
                        OrderNative orderNative = OrderNative.fromOrder(clientOrder);
                        if(orderNative != null) clientOrders.add(orderNative.toJson());
                    }
                });
        return clientOrders;
    }

    static private ReplyNative updateOrder(OrderPortalGrpc.OrderPortalBlockingStub blockingStub, OrderNative orderNative) {
        if (!myHashes.containsKey(orderNative.getOID())) {
            Optional<OrderNative> optionalOrderNative = retrieveOrder(blockingStub,
                                                                      orderNative.getOID());
            optionalOrderNative.ifPresent((oldOrder) -> {
                System.out.println("Como você ainda não tinha se relacionado com o OID "
                                           + orderNative.getOID()
                                           + ", a atualização será baseada nesta ordem " + oldOrder.toJson());
                if (!"0".equals(oldOrder.getOID())) {myHashes.put(oldOrder.getOID(), oldOrder.getHash());}
            });
        }
        orderNative.setUpdatedVersionHash(myHashes.getOrDefault(
                orderNative.getOID(), ""));
        ReplyNative replyNative = ReplyNative.fromReply(blockingStub.updateOrder(orderNative.toOrder()));
        myHashes.put(orderNative.getOID(), orderNative.getHash());
        return replyNative;
    }

    private String login(AdminPortalGrpc.AdminPortalBlockingStub blockingStub, Scanner scanner) {
        System.out.println("Por favor, autentique o cliente antes de prosseguir.");

        int attempts = 5;
        do {
            System.out.print("Escreva o ID do cliente: ");
            String clientId = scanner.nextLine();
            if (!"0".equals(blockingStub.retrieveClient(ID.newBuilder().setID(clientId).build()).getCID())) {
                System.out.println("Login efetuado com sucesso!");
                return clientId;
            }
            System.out.println("ID invalido. " + --attempts + " tentativas restantes.");
        } while (attempts > 0);

        return null;
    }

    private OrderItemNative addProductToOrder() {
        OrderItemNative orderItemNative = OrderItemNative.builder().build();
        System.out.print("Escreva o ID do produto que deseja adicionar: ");
        orderItemNative.setPID(scanner.nextLine());

        Integer inputQuantity = null;
        while (inputQuantity == null) {
            try {
                System.out.print("Escreva a quantidade desejada do produto: ");
                inputQuantity = Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException formatException) {
                System.out.println("Escreva um inteiro...");
            }
        }
        orderItemNative.setQuantity(inputQuantity);


        Double inputPrice = null;
        while (inputPrice == null) {
            try {
                System.out.print("Escreva o preco a ser pago pelo produto: ");
                inputPrice = Double.parseDouble(scanner.nextLine());
            } catch (NumberFormatException formatException) {
                System.out.println("Escreva um double...");
            }
        }
        orderItemNative.setPrice(inputPrice);

        System.out.print("Escreva o codigo de fidelidade: ");
        orderItemNative.setFidelityCode(scanner.nextLine());

        return orderItemNative;
    }
}