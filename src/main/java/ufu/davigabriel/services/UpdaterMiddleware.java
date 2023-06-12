package ufu.davigabriel.services;

import lombok.Getter;
import ufu.davigabriel.server.ID;
import ufu.davigabriel.server.distributedDatabase.RatisClient;

import java.io.IOException;

public class UpdaterMiddleware {
    @Getter
    private RatisClient[] ratisClients;

    public UpdaterMiddleware() {
        this.ratisClients = new RatisClient[]{new RatisClient(0), new RatisClient(1)};
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
    protected RatisClient getRatisClientFromID(String id) { return this.ratisClients[Integer.parseInt(id) % 2]; }
}
