package ufu.davigabriel.exceptions;

import io.grpc.stub.StreamObserver;
import lombok.NoArgsConstructor;
import ufu.davigabriel.models.ReplyNative;
import ufu.davigabriel.server.Reply;

@NoArgsConstructor
public abstract class PortalException extends Exception {
    public PortalException(String message) {
        super(message);
    }

    public void replyError(StreamObserver responseObserver) {
        this.replyError(responseObserver, ReplyNative.ERRO_DESCONHECIDO);
    }

    public Reply getErrorReply(ReplyNative replyNative) {
        return Reply.newBuilder()
                .setError(replyNative.getError())
                .setDescription(replyNative.getDescription() + this.getMessage())
                .build();
    }

    public void replyError(StreamObserver responseObserver, ReplyNative replyNative) {
        responseObserver.onNext(getErrorReply(replyNative));
    }
}
