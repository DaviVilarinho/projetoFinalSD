package ufu.davigabriel.models;

import com.google.gson.Gson;
import com.google.gson.annotations.JsonAdapter;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ufu.davigabriel.server.Order;

import java.util.ArrayList;

@Builder
@Setter
@Getter
@ToString
public class OrderNative {
    private String OID;
    private String CID;
    private ArrayList<OrderItemNative> products;

    public static OrderNative fromOrder(Order order) {
        return new Gson().fromJson(order.getData(), OrderNative.class);
    }

    public Order toOrder() {
        return Order.newBuilder()
                .setOID(getOID())
                .setCID(getCID())
                .setData(new Gson().toJson(this))
                .build();
    }

    public static OrderNative generateEmptyOrderNative(){
        return OrderNative.builder()
                .OID("0")
                .CID("")
                .build();
    }

    public static OrderNative fromJson(String json) {
        return new Gson().fromJson(json, OrderNative.class);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
