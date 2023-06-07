package utils;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import ufu.davigabriel.models.ClientNative;
import ufu.davigabriel.models.OrderNative;
import ufu.davigabriel.models.ProductNative;

import java.util.List;

@Getter
@Setter
@Builder
public class RandomOrderTriple {
    List<ProductNative> randomProductsNative;
    ClientNative randomClientNative;
    OrderNative randomOrderNative;
}
