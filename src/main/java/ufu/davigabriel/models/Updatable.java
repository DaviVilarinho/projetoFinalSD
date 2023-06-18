package ufu.davigabriel.models;

public interface Updatable {
    String getHash(); // necessário para determinar qual é a versão
    String getCacheKey(); // necessário para saber em qual entrada está na cache (no caso são os CID/PID/OID)
    String getUpdatedVersionHash(); // necessário para saber sobre o que o cliente está manifestando update
}
