package ufu.davigabriel.models;

import org.apache.ratis.protocol.RaftPeer;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GlobalVarsService {
    public static final String[] PARTITION_RATIS_IDS = new String[]{"______particao_0",
            "______particao_1"};
    public static List<InetSocketAddress> RATIS_DATABASE_PORTS_PARTITION_1 = List.of(
            new InetSocketAddress("127.0.0.1", 57333),
            new InetSocketAddress("127.0.0.1", 57334),
            new InetSocketAddress("127.0.0.1", 57335));
    public static List<InetSocketAddress> RATIS_DATABASE_PORTS_PARTITION_2 = List.of(
            new InetSocketAddress("127.0.0.1", 57336),
            new InetSocketAddress("127.0.0.1", 57337),
            new InetSocketAddress("127.0.0.1", 57338));
    public static final List<InetSocketAddress>[] RATIS_DATABASE_PARTITIONS =
            new List[]{RATIS_DATABASE_PORTS_PARTITION_1, RATIS_DATABASE_PORTS_PARTITION_2};

    public static String STORAGE_DIR_DEFAULT = "/tmp";
    private static GlobalVarsService instance;

    public static GlobalVarsService getInstance() {
        if (instance == null) {
            instance = new GlobalVarsService();
        }
        return GlobalVarsService.instance;
    }

    public static int getPeersQuantity() {
        return RATIS_DATABASE_PORTS_PARTITION_2.size();
    }

    public static Map<String, InetSocketAddress> getPeerIdAddressesFromPartition(int partition) {
        Map<String, InetSocketAddress> id2addr = new HashMap<>();
        IntStream.range(1, 4).forEach(i -> id2addr.put(String.format("p%d", i),
                                                       RATIS_DATABASE_PARTITIONS[partition].get(i - 1)));
        return id2addr;
    }

    public static List<RaftPeer> getRaftPeersFromPartition(int partition) {
        return getPeerIdAddressesFromPartition(partition).entrySet().stream()
                .map(peerPartitionKeyAddress -> RaftPeer.newBuilder().setId(
                        peerPartitionKeyAddress.getKey()).setAddress(peerPartitionKeyAddress.getValue()).build())
                .collect(Collectors.toList());
    }
}