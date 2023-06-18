package ufu.davigabriel.services;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.tuple.Pair;
import ufu.davigabriel.exceptions.IllegalVersionPortalItemException;
import ufu.davigabriel.exceptions.NotFoundItemInPortalException;
import ufu.davigabriel.models.Updatable;

import java.util.HashMap;

@Getter
@Setter
@AllArgsConstructor
public class BaseCacheService {
    private HashMap<String, Pair<String, Long>> cacheTable;
    private long TTL;

    public BaseCacheService() {
        this.cacheTable = new HashMap<>();
        this.TTL = 30L * 1000L;
    }

    public void throwNotFoundItemIfOldOrNotFoundHash(Updatable updatable) throws NotFoundItemInPortalException {
        throwNotFoundItemIfOldOrNotFoundHash(updatable.getCacheKey());
    }

    public boolean isCacheOldOrMissing(String key) {
        if (!cacheTable.containsKey(key)) {
            return true;
        }
        return System.currentTimeMillis() - cacheTable.get(key).getRight() > TTL;
    }

    public void throwNotFoundItemIfOldOrNotFoundHash(String key) throws NotFoundItemInPortalException {
        if (!cacheTable.containsKey(key)) {
            throw new NotFoundItemInPortalException();
        }
        if (isCacheOldOrMissing(key)) {
            cacheTable.remove(key);
            throw new NotFoundItemInPortalException();
        }
    }

    public void addToCache(Updatable updatable) {
        this.cacheTable.put(updatable.getCacheKey(), Pair.of(updatable.getHash(), System.currentTimeMillis()));
    }

    public void removeFromCache(Updatable updatable) { removeFromCache(updatable.getCacheKey()); }
    public void removeFromCache(String id) { this.cacheTable.remove(id); }

    public void throwIfNotUpdatable(Updatable newUpdate) throws NotFoundItemInPortalException, IllegalVersionPortalItemException {
        throwNotFoundItemIfOldOrNotFoundHash(newUpdate.getCacheKey()); // not fresh
        String hashOnCache = cacheTable.get(newUpdate.getCacheKey()).getLeft();
        if (!hashOnCache.equals(newUpdate.getUpdatedVersionHash())) {
            System.out.println("Novo update referencia hash --\"" + newUpdate.getUpdatedVersionHash() + "\"-- que é diferente de " + hashOnCache);
            throw new IllegalVersionPortalItemException(); // not the correct version
        }
    }
}
