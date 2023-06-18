package ufu.davigabriel.models;

import com.google.gson.Gson;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.ratis.io.MD5Hash;
import ufu.davigabriel.server.Product;

@Getter
@Setter
@Builder(toBuilder = true)
@ToString
public class ProductNative implements Updatable {
    private String PID;
    private String name;
    private int quantity;
    private double price;
    private String description;
    private String updatedVersionHash;

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

    @Override
    public String getHash() {
        return MD5Hash.digest(
                (this.getPID() + this.getName() + String.format("%d%.2f", this.quantity, this.price) + this.getDescription()).getBytes()).toString();
    }

    @Override
    public String getCacheKey() {
        return getPID();
    }
}




