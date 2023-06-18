package ufu.davigabriel.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import ufu.davigabriel.exceptions.IllegalVersionPortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.models.Updatable;

import java.util.HashMap;

/**
 * BaseCacheService é uma superclasse que fornece serviços de
 * CACHE
 * ou seja salva um hash, o id que o dá e por quanto tempo isso é válido
 * em seguida quando é chamado requisições é possível verificar:
 *  -   versão de acordo com esperado
 *  -   se é velha
 */
@Getter
@Setter
@AllArgsConstructor
public class BaseCacheService {
    private HashMap<String, Pair<String, Long>> hashCacheTable;
    private long TTL;

    public BaseCacheService() {
        this.hashCacheTable = new HashMap<>();
        this.TTL = 30L * 1000L;
    }

    public void throwNotFoundItemIfOldOrNotFoundHash(Updatable updatable) throws NotFoundItemInPortalException {
        throwNotFoundItemIfOldOrNotFoundHash(updatable.getCacheKey());
    }

    public boolean isCacheOldOrMissing(String key) {
        if (!hashCacheTable.containsKey(key)) {
            return true;
        }
        return System.currentTimeMillis() - hashCacheTable.get(key).getRight() > TTL;
    }

    public void throwNotFoundItemIfOldOrNotFoundHash(String key) throws NotFoundItemInPortalException {
        if (!hashCacheTable.containsKey(key)) {
            throw new NotFoundItemInPortalException();
        }
        if (isCacheOldOrMissing(key)) {
            hashCacheTable.remove(key);
            throw new NotFoundItemInPortalException();
        }
    }

    public void addToCache(Updatable updatable) {
        this.hashCacheTable.put(updatable.getCacheKey(), Pair.of(updatable.getHash(), System.currentTimeMillis()));
    }

    public void removeFromCache(Updatable updatable) { removeFromCache(updatable.getCacheKey()); }
    public void removeFromCache(String id) { this.hashCacheTable.remove(id); }

    /*
        Atualização Causal
        Se não refletir a escrita que o cliente diz que usou quando atualizou pelo atributo updatedVersionHash
        NÃO deixa atualizar
     */
    public void throwIfNotUpdatable(Updatable newUpdate) throws NotFoundItemInPortalException, IllegalVersionPortalItemException {
        throwNotFoundItemIfOldOrNotFoundHash(newUpdate.getCacheKey()); // not fresh
        String hashOnCache = hashCacheTable.get(newUpdate.getCacheKey()).getLeft();
        if (!hashOnCache.equals(newUpdate.getUpdatedVersionHash())) {
            System.out.println("Novo update referencia hash --\"" + newUpdate.getUpdatedVersionHash() + "\"-- que é diferente de " + hashOnCache);
            throw new IllegalVersionPortalItemException(); // not the correct version
        }
    }
}
