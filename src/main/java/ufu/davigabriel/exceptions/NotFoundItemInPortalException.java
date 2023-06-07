package ufu.davigabriel.exceptions;

import io.grpc.stub.StreamObserver;
import ufu.davigabriel.models.ReplyNative;

public class NotFoundItemInPortalException extends PortalException {
    @Override
    public void replyError(StreamObserver responseObserver) {
        replyError(responseObserver, ReplyNative.INEXISTENTE);
    }
}
