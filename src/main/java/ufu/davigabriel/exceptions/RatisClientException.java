package ufu.davigabriel.exceptions;

import io.grpc.stub.StreamObserver;
import lombok.NoArgsConstructor;
import ufu.davigabriel.models.ReplyNative;

@NoArgsConstructor
public class RatisClientException extends PortalException {
    public RatisClientException(String message) { super(message); }
    @Override
    public void replyError(StreamObserver responseObserver) {
        super.replyError(responseObserver, ReplyNative.ERRO_PROTOCOLOS);
    }
}
