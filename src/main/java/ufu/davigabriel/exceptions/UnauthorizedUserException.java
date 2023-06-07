package ufu.davigabriel.exceptions;

import io.grpc.stub.StreamObserver;
import ufu.davigabriel.models.ReplyNative;

public class UnauthorizedUserException extends PortalException {
    @Override
    public void replyError(StreamObserver responseObserver) {
        super.replyError(responseObserver, ReplyNative.NAO_LOGADO);
    }
}
