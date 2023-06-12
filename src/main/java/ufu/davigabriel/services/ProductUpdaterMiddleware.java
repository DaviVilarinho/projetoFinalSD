package ufu.davigabriel.services;

import ufu.davigabriel.exceptions.DuplicatePortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.exceptions.RatisClientException;
import ufu.davigabriel.models.ProductNative;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.Product;

import java.nio.charset.Charset;

public class ProductUpdaterMiddleware extends UpdaterMiddleware implements IProductProxyDatabase {
    private static ProductUpdaterMiddleware instance;
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
    public void createProduct(Product product) throws DuplicatePortalItemException, RatisClientException {
        if (productCacheService.hasProduct(product)) {
            throw new DuplicatePortalItemException("Produto já existe: " + product.toString());
        }
        getRatisClientFromID(product.getPID()).add(getProductStorePath(product),
                                                ProductNative.fromProduct(product).toJson());
        productCacheService.createProduct(product);
    }

    @Override
    public void updateProduct(Product product) throws NotFoundItemInPortalException, RatisClientException {
        if (!productCacheService.hasProduct(product)) {
            retrieveProduct(product); // TODO avaliar se está lançando o NOTFOUND
        }
        productCacheService.updateProduct(ProductNative.fromJson(getRatisClientFromID(product.getPID()).update(
                getProductStorePath(product), product.toString()).getMessage().getContent().toString(
                Charset.defaultCharset())).toProduct()); // TODO avaliar get nulo
    }

    public Product retrieveProduct(Product product) throws NotFoundItemInPortalException, RatisClientException {
        return retrieveProduct(ID.newBuilder().setID(product.getPID()).build());
    }

    @Override
    public Product retrieveProduct(ID id) throws NotFoundItemInPortalException, RatisClientException {
        try {
            return productCacheService.retrieveProduct(id);
        } catch (NotFoundItemInPortalException notFoundItemInPortalException) {
            Product product = ProductNative.fromJson(getRatisClientFromID(id.getID()).get(
                    getStorePath(id.getID())).getMessage().getContent().toString(
                    Charset.defaultCharset())).toProduct(); // TODO avaliar get nulo
            try {
                productCacheService.createProduct(product);
            } catch (DuplicatePortalItemException e) {
                throw new RatisClientException("Erro de sincronizacao interna com a database " + id.getID());
            }

            return product;
        }
    }

    @Override
    public void deleteProduct(ID id) throws NotFoundItemInPortalException, RatisClientException {
        if (!productCacheService.hasProduct(id.getID())) {
            retrieveProduct(id); // TODO avaliar se está lançando o NOTFOUND
        }
        productCacheService.deleteProduct(ID.newBuilder().setID(ProductNative.fromJson(
                getRatisClientFromID(id.getID()).del(
                        getStorePath(id.getID().toString())).getMessage().getContent().toString(
                        Charset.defaultCharset())).getPID()).build()); // TODO avaliar get nulo
    }
}
