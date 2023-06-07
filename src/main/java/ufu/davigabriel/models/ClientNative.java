package ufu.davigabriel.models;

import com.google.gson.Gson;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import ufu.davigabriel.server.Client;

@Getter
@Setter
@Builder(toBuilder = true)
@ToString
public class ClientNative {
    private String CID;
    private String name;
    private String zipCode;

    public Client toClient() {
        return Client.newBuilder()
                .setCID(getCID())
                .setData(new Gson().toJson(this))
                .build();
    }

    public static ClientNative fromClient(Client client) {
        return new Gson().fromJson(client.getData(), ClientNative.class);
    }


    public static ClientNative generateEmptyClientNative(){
        return ClientNative.builder()
                .CID("0")
                .name("")
                .zipCode("")
                .build();
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public static ClientNative fromJson(String json) {
        return new Gson().fromJson(json, ClientNative.class);
    }
}