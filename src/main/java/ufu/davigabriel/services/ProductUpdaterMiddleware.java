package ufu.davigabriel.services;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufu.davigabriel.exceptions.*;
import ufu.davigabriel.models.ProductNative;
import ufu.davigabriel.server.Client;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Product;

import java.nio.charset.Charset;

public class ProductUpdaterMiddleware extends UpdaterMiddleware implements IProductProxyDatabase {
    private static ProductUpdaterMiddleware instance;
    private static Logger logger = LoggerFactory.getLogger(ProductUpdaterMiddleware.class);
    private ProductCacheService productCacheService = ProductCacheService.getInstance();
    public static ProductUpdaterMiddleware getInstance() {
        if (instance == null){
            instance = new ProductUpdaterMiddleware();
        }
        return instance;
    }

    public String getProductStorePath(Product product) {
        return super.getStorePath(product.getPID());
    }

    @Override
    public String getSelfSavePath() {
        return "products/";
    }

    @Override
    public void createProduct(Product product) throws DuplicatePortalItemException, BadRequestException, RatisClientException {
        try {
            retrieveProduct(product);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            System.out.println("Não há produto " + product.getPID() + ", prosseguindo criação");
        }
        if (productCacheService.hasProduct(product)) {
            throw new DuplicatePortalItemException("Produto já existe");
        }
        try {
            if (!getRatisClientFromID(product.getPID()).add(getProductStorePath(product), ProductNative.fromProduct(product).toJson()).isSuccess()) {
                throw new DuplicatePortalItemException();
            }
            productCacheService.createProduct(product);
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + product);
            throw new BadRequestException();
        }
    }

    @Override
    public void updateProduct(Product product) throws NotFoundItemInPortalException, RatisClientException, BadRequestException, IllegalVersionPortalItemException {
        retrieveProduct(product);
        productCacheService.throwIfNotUpdatable(ProductNative.fromProduct(product));
        try {
            if (!getRatisClientFromID(product.getPID()).update(
                    getProductStorePath(product), product.toString()).isSuccess()) {
                throw new NotFoundItemInPortalException();
            }
            productCacheService.updateProduct(product);
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + product);
            throw new BadRequestException();
        }
    }

    public Product retrieveProduct(Product product) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        return retrieveProduct(ID.newBuilder().setID(product.getPID()).build());
    }

    @Override
    public Product retrieveProduct(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        try {
            return productCacheService.retrieveProduct(id);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            System.out.println("ID não encontrado, tentando buscar no bd " + id);
        }

        try {
            String queryProduct = getRatisClientFromID(id.getID()).get(getStorePath(id.getID())).getMessage().getContent().toString(Charset.defaultCharset());
            System.out.println("Banco encontrou product: " + queryProduct);
            String onlyJson = queryProduct.split(":", 2)[1];
            if ("null".equals(onlyJson)) {
                throw new NotFoundItemInPortalException(id.getID());
            }
            Product product = new Gson().fromJson(onlyJson, Product.class);
            productCacheService.createProduct(product);
            return product;
        } catch (JsonSyntaxException jsonSyntaxException) {
            throw new NotFoundItemInPortalException();
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + id);
            throw new BadRequestException();
        } catch (DuplicatePortalItemException e) {
            e.printStackTrace();
            throw new RatisClientException("Erro de sincronizacao interna com a database " + id.getID());
        }
    }

    @Override
    public void deleteProduct(ID id) throws NotFoundItemInPortalException, RatisClientException, BadRequestException {
        retrieveProduct(id);
        try {
            if (!getRatisClientFromID(id.getID()).del(getStorePath(id.getID())).isSuccess()) {
                throw new NotFoundItemInPortalException();
            }
            productCacheService.deleteProduct(id);
        } catch (IllegalStateException illegalStateException) {
            illegalStateException.printStackTrace();
            System.out.println("Erro json inválido: " + id);
            throw new BadRequestException();
        }
    }
}
