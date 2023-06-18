package ufu.davigabriel.models;

public interface Updatable {
    String getHash();
    String getCacheKey();
    String getUpdatedVersionHash();
}
