package ufu.davigabriel.exceptions;

import io.grpc.stub.StreamObserver;
import lombok.NoArgsConstructor;
import ufu.davigabriel.models.ReplyNative;
@NoArgsConstructor
public class BadRequestException extends PortalException {
    public BadRequestException(String message) {
        super(message);
    }

    @Override
    public void replyError(StreamObserver responseObserver) {
        super.replyError(responseObserver, ReplyNative.BAD_REQUEST);
    }
}
