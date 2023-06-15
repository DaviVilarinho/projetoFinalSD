package ufu.davigabriel.exceptions;

import io.grpc.stub.StreamObserver;
import lombok.NoArgsConstructor;
import ufu.davigabriel.models.ReplyNative;

@NoArgsConstructor
public class NotFoundItemInPortalException extends PortalException {
    public NotFoundItemInPortalException(String message) {
        super(message);
    }

    @Override
    public void replyError(StreamObserver responseObserver) {
        replyError(responseObserver, ReplyNative.INEXISTENTE);
    }
}
