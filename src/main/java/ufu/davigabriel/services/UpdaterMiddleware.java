package ufu.davigabriel.services;

import lombok.Getter;
import ufu.davigabriel.server.distributedDatabase.RatisClient;

import java.io.IOException;

public class UpdaterMiddleware {
    private static UpdaterMiddleware instance;
    @Getter
    private RatisClient[] ratisClients;

    public UpdaterMiddleware() {
        if (instance == null) {
            this.ratisClients = new RatisClient[]{new RatisClient(0), new RatisClient(1)};
        }
    }

    public static UpdaterMiddleware getInstance() {
        if (instance == null) {
            instance = new UpdaterMiddleware();
        }
        return instance;
    }

    public void closeClients() throws IOException {
        for (RatisClient client : this.getRatisClients()) {
            client.close();
        }
    }

    public String getSelfSavePath() {
        return ""; // directory/to/save/ (with a slash in the end
    }

    public String getStorePath(String id) {
        return getSelfSavePath() + id;
    }
}
