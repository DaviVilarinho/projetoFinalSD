package ufu.davigabriel.server.distributedDatabase;

import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcFactory;
import org.apache.ratis.protocol.*;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import ufu.davigabriel.exceptions.RatisClientException;
import ufu.davigabriel.models.GlobalVarsService;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RatisClient {
    private int partitionPeer;
    private final RaftClient client;

    public RatisClient(int partitionPeer) {
        this.partitionPeer = partitionPeer;

        final RaftGroup raftGroup = RaftGroup.valueOf(
                RaftGroupId.valueOf(ByteString.copyFromUtf8(GlobalVarsService.PARTITION_RATIS_IDS[partitionPeer])),
                GlobalVarsService.getPeerIdAddressesFromPartition(partitionPeer).entrySet().stream().map(
                        e -> RaftPeer.newBuilder().setId(e.getKey()).setAddress(e.getValue()).build()).collect(
                        Collectors.toList()));
        RaftProperties raftProperties = new RaftProperties();

        this.client = RaftClient.newBuilder().setProperties(raftProperties).setRaftGroup(raftGroup).setClientRpc(
                new GrpcFactory(new Parameters()).newRaftClientRpc(ClientId.randomId(), raftProperties)).build();
    }

    public RaftClientReply add(String key, String value) throws RatisClientException {
        try {
            return client.io().send(Message.valueOf("add:" + key + ":" + value));
        } catch (IOException ioException) {
            throw new RatisClientException();
        }
    }

    public RaftClientReply update(String key, String value) throws RatisClientException {
        try {
            return client.io().send(Message.valueOf("update:" + key + ":" + value));
        } catch (IOException ioException) {
            throw new RatisClientException();
        }
    }

    public CompletableFuture<RaftClientReply> addAsync(String key, String value) {
        return client.async().send(Message.valueOf("add:" + key + ":" + value));
    }

    public RaftClientReply get(String key) throws RatisClientException {
        try {
            return client.io().sendReadOnly(Message.valueOf("get:" + key));
        } catch (IOException ioException) {
            throw new RatisClientException();
        }
    }

    public RaftClientReply getStale(String key, String peerId) throws RatisClientException {
        try {
            return client.io().sendStaleRead(Message.valueOf("get:" + key), 0, RaftPeerId.valueOf(peerId));
        } catch (IOException ioException) {
            throw new RatisClientException();
        }
    }

    public RaftClientReply del(String key) throws RatisClientException {
        try {
            return client.io().sendReadOnly(Message.valueOf("del:" + key));
        } catch (IOException ioException) {
            throw new RatisClientException();
        }
    }

    public RaftClientReply clear() throws RatisClientException {
        try {
            return client.io().send(Message.valueOf("clear"));
        } catch (IOException ioException) {
            throw new RatisClientException();
        }
    }

    public void close() throws IOException {
        client.close();
    }
}
