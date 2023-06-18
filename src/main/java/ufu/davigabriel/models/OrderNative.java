package ufu.davigabriel.models;

import com.google.gson.Gson;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.ratis.io.MD5Hash;
import ufu.davigabriel.server.Order;
import ufu.davigabriel.services.BaseCacheService;

import java.util.ArrayList;

@Builder
@Setter
@Getter
@ToString
public class OrderNative implements Updatable {
    private String OID;
    private String CID;
    private ArrayList<OrderItemNative> products;
    private String updatedVersionHash;


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

    @Override
    public String getHash() {
        return MD5Hash.digest((OID + CID + products.toString()).getBytes()).toString();
    }

    @Override
    public String getCacheKey() {
        return getOID();
    }

    public String toJson() {
        return new Gson().toJson(this);
    }
}
