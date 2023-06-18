package ufu.davigabriel.exceptions;

import io.grpc.stub.StreamObserver;
import lombok.NoArgsConstructor;
import ufu.davigabriel.models.ReplyNative;

@NoArgsConstructor
public class IllegalVersionPortalItemException extends PortalException {

    public IllegalVersionPortalItemException(String message) { super(message); }
    @Override
    public void replyError(StreamObserver responseObserver) {
        ReplyNative replyNative = ReplyNative.VERSAO_CONFLITANTE;
        System.out.println(replyNative.getDescription());
        replyError(responseObserver, replyNative);
    }
}
