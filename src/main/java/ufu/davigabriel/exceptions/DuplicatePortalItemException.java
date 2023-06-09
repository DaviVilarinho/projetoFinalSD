package ufu.davigabriel.exceptions;

import io.grpc.stub.StreamObserver;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import ufu.davigabriel.models.ReplyNative;

@NoArgsConstructor
public class DuplicatePortalItemException extends PortalException {

    public DuplicatePortalItemException(String message) { super(message); }
    @Override
    public void replyError(StreamObserver responseObserver) {
        ReplyNative replyNative = ReplyNative.DUPLICATA;
        System.out.println(replyNative.getDescription());
        replyError(responseObserver, replyNative);
    }
}
