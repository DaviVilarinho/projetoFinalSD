package ufu.davigabriel.server.distributedDatabase;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.ratis.conf.RaftProperties;
import org.apache.ratis.grpc.GrpcConfigKeys;
import org.apache.ratis.protocol.RaftGroup;
import org.apache.ratis.protocol.RaftGroupId;
import org.apache.ratis.protocol.RaftPeer;
import org.apache.ratis.protocol.RaftPeerId;
import org.apache.ratis.server.RaftServer;
import org.apache.ratis.server.RaftServerConfigKeys;
import org.apache.ratis.thirdparty.com.google.protobuf.ByteString;
import org.apache.ratis.util.LifeCycle;
import ufu.davigabriel.models.GlobalVarsService;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Getter
@Setter
@Builder
public class RatisServer {
    private String raftGroupId;

    // Parametros: particao
    public static void main(String[] args) throws IOException, InterruptedException {
        int ratisPartitionNumber = 0;
        String ratisPartitionPeerId = args[1];
        try {
            ratisPartitionNumber = Integer.parseInt(args[0]);
            assert ratisPartitionNumber == 0 || ratisPartitionNumber == 1;
        } catch (NumberFormatException | AssertionError notTheRatisConfigurationException) {
            System.out.println("Insira uma particao (0 e 1)");
            System.exit(1);
        }

        RaftPeerId myId = RaftPeerId.valueOf(ratisPartitionPeerId);

        List<RaftPeer> addresses = GlobalVarsService.getRaftPeersFromPartition(ratisPartitionNumber);

        if (addresses.stream().noneMatch(p -> p.getId().equals(myId))) {
            System.out.println("Identificador " + ratisPartitionPeerId + " é inválido ou não " + "pertence à particao " + ratisPartitionNumber);
            System.exit(1);
        }

        final int serverPort = GlobalVarsService.getPeerIdAddressesFromPartition(ratisPartitionNumber).get(ratisPartitionPeerId).getPort();

        RaftProperties properties = new RaftProperties();
        properties.setInt(GrpcConfigKeys.OutputStream.RETRY_TIMES_KEY, Integer.MAX_VALUE);
        GrpcConfigKeys.Server.setPort(properties, serverPort);
        RaftServerConfigKeys.setStorageDir(properties, Collections.singletonList(new File(GlobalVarsService.STORAGE_DIR_DEFAULT + "/" + ratisPartitionNumber + "/" + myId)));

        // Join the group of processes.
        final RaftGroup raftGroup = RaftGroup.valueOf(RaftGroupId.valueOf(ByteString.copyFromUtf8(GlobalVarsService.PARTITION_RATIS_IDS[ratisPartitionNumber])), addresses);
        RaftServer raftServer = RaftServer.newBuilder().setServerId(myId).setStateMachine(new BaseStateMachine()).setProperties(properties).setGroup(raftGroup).build();
        raftServer.start();
        while (raftServer.getLifeCycleState() != LifeCycle.State.CLOSED) {
            TimeUnit.SECONDS.sleep(1);
        }
    }
}
