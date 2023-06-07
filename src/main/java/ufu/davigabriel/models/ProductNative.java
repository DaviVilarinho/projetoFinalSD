package ufu.davigabriel.models;

import com.google.gson.Gson;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ufu.davigabriel.server.Product;

@Getter
@Setter
@Builder(toBuilder = true)
@ToString
public class ProductNative {
    private String PID;
    private String name;
    private int quantity;
    private double price;
    private String description;

    public Product toProduct() {
        return Product.newBuilder()
                .setPID(getPID())
                .setData(new Gson().toJson(this.toBuilder().build()))
                .build();
    }

    public static ProductNative fromProduct(Product product) {
        return new Gson().fromJson(product.getData(), ProductNative.class);
    }

    public static ProductNative generateEmptyProductNative(){
        return ProductNative.builder()
                .PID("0")
                .name("")
                .quantity(-1)
                .price(-1)
                .description("")
                .build();
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static ProductNative fromJson(String json) {
        return new Gson().fromJson(json, ProductNative.class);
    }
}




