package ufu.davigabriel.exceptions;

import io.grpc.stub.StreamObserver;
import ufu.davigabriel.models.ReplyNative;

public class DuplicatePortalItemException extends PortalException {
    @Override
    public void replyError(StreamObserver responseObserver) {
        ReplyNative replyNative = ReplyNative.DUPLICATA;
        System.out.println(replyNative.getDescription());
        replyError(responseObserver, replyNative);
    }
}
