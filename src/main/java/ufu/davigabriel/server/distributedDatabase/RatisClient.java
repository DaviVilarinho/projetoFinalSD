package ufu.davigabriel.server.distributedDatabase;

import org.apache.ratis.client.RaftClient;
import org.apache.ratis.conf.Parameters;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcFactory;
import org.apache.ratis.protocol.*;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import ufu.davigabriel.models.GlobalVarsService;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class RatisClient {
    private final int partitionPeer;
    private final RaftClient client;

    public RatisClient(int partitionPeer) {
        this.partitionPeer = partitionPeer;

        final RaftGroup raftGroup =
                RaftGroup.valueOf(RaftGroupId.valueOf(
                                          ByteString.copyFromUtf8(GlobalVarsService.PARTITION_RATIS_IDS[partitionPeer])),
                                  GlobalVarsService.getPeerIdAddressesFromPartition(
                                          partitionPeer).entrySet().stream().map(e -> RaftPeer.newBuilder().setId(
                                          e.getKey()).setAddress(e.getValue()).build()).collect(Collectors.toList()));
        RaftProperties raftProperties = new RaftProperties();

        this.client =
                RaftClient.newBuilder().setProperties(raftProperties).setRaftGroup(raftGroup).setClientRpc(
                        new GrpcFactory(new Parameters()).newRaftClientRpc(ClientId.randomId(),
                                                                           raftProperties)).build();
    }

    public RaftClientReply add(String key, String value) throws IOException {
        return client.io().send(Message.valueOf("add:" + key + ":" + value));
    }

    public CompletableFuture<RaftClientReply> addAsync(String key, String value) throws IOException {
        return client.async().send(Message.valueOf("add:" + key + ":" + value));
    }

    public RaftClientReply get(String key) throws IOException {
        return client.io().send(Message.valueOf("get:" + key));
    }

    public RaftClientReply getStale(String key, String peerId) throws IOException {
        return client.io().sendStaleRead(Message.valueOf("get:" + key), 0, RaftPeerId.valueOf(peerId));
    }

    public RaftClientReply del(String key) throws IOException {
        return client.io().send(Message.valueOf("del:" + key));
    }

    public RaftClientReply clear() throws IOException {
        return client.io().send(Message.valueOf("clear"));
    }

    public void close() throws IOException {
        client.close();
    }
}
