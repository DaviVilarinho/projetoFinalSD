package utils;

import net.bytebuddy.utility.RandomString;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.models.OrderItemNative;
import ufu.davigabriel.models.OrderNative;
import ufu.davigabriel.models.ProductNative;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class RandomUtils {
    public static ClientNative generateRandomClient() {
        return ClientNative.builder()
                .CID(RandomString.make(32).strip().trim())
                .name(RandomString.make(16))
                .zipCode(RandomString.make(8))
                .build();
    }

    public static ProductNative generateRandomProduct() {
        return ProductNative.builder()
                .PID(RandomString.make(32).strip().trim())
                .name(RandomString.make(16))
                .price(new Random().nextDouble())
                .quantity(Math.abs(new Random().nextInt()) + 1)
                .description(RandomString.make(64))
                .build();
    }

    public static ProductNative generateEmptyRandomProduct() {
        return generateRandomProduct().toBuilder()
                .quantity(0)
                .build();
    }

    public static OrderNative generateRandomOrderNative(List<ProductNative> randomProductsNative, String clientId) {
        return OrderNative.builder()
                .OID(RandomString.make(32).strip().trim())
                .CID(clientId)
                .products(randomProductsNative.stream().map(
                                productNative -> OrderItemNative.convertProductNative(
                                        productNative,
                                        RandomString.make(8),
                                        productNative.getQuantity() / (new Random().nextInt(productNative.getQuantity() + 1)),
                                        10))
                        .collect(Collectors.toCollection(ArrayList::new)))
                .build();
    }

    public static RandomOrderTriple generateRandomValidOrder() {
        ClientNative randomClientNative = generateRandomClient();
        List<ProductNative> randomProductsNative = List.of(generateRandomProduct(), generateRandomProduct());
        return RandomOrderTriple.builder()
                .randomClientNative(randomClientNative)
                .randomProductsNative(randomProductsNative)
                .randomOrderNative(generateRandomOrderNative(randomProductsNative, randomClientNative.getCID()))
                .build();
    }
}
