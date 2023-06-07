package ufu.davigabriel.models;

import lombok.Getter;
import lombok.ToString;
import ufu.davigabriel.server.Reply;

import java.util.Arrays;

@Getter
@ToString
public enum ReplyNative {
    SUCESSO(0, ""),
    BAD_REQUEST(400, "Bad request."),
    NAO_LOGADO(401, "Usuario nao existe."),
    INEXISTENTE(404, "Item nao existe."),
    DUPLICATA(422, "Item ja existe."),
    ERRO_DESCONHECIDO(500, "Falha interna."),
    ERRO_MQTT(502, "Erro no servidor Mosquitto.");

    private final int error;
    private final String description;

    ReplyNative(int error, String description) {
        this.error = error;
        this.description = description;
    }

    static public ReplyNative fromReply(Reply reply) {
        return Arrays.stream(ReplyNative.values())
                .filter(replyNative -> replyNative.getError() == reply.getError())
                .findFirst()
                .orElse(ERRO_DESCONHECIDO);
    }
}