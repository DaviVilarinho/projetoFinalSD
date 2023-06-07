package ufu.davigabriel.models;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@Builder
@ToString
public class OrderItemNative {
    private String PID;
    private String fidelityCode;
    private int quantity;
    private double price;

    public static OrderItemNative convertProductNative(ProductNative productNative, String fidelityCode, int quantity, double price) {
        return OrderItemNative.builder()
                .PID(productNative.getPID())
                .fidelityCode(fidelityCode)
                .quantity(quantity)
                .price(price)
            .build();
    }
}
