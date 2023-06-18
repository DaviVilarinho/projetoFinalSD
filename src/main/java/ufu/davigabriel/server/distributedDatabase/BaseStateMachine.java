package ufu.davigabriel.server.distributedDatabase;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static org.iq80.leveldb.impl.Iq80DBFactory.factory;
import org.apache.ratis.proto.*;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.statemachine.TransactionContext;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBFactory;
import org.iq80.leveldb.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufu.davigabriel.server.AdminPortalServer;

import javax.swing.text.html.Option;


public class BaseStateMachine extends org.apache.ratis.statemachine.impl.BaseStateMachine {
        private final Map<String, String> key2values = new ConcurrentHashMap<>();
        private String partitionPeerId;
        private int partitionNumber;
        private final Options dbOptions;

        public BaseStateMachine(String partitionPeerId, int partitionNumber) {
                this.partitionPeerId = partitionPeerId;
                this.partitionNumber = partitionNumber;
                this.dbOptions = new Options();
                this.dbOptions.createIfMissing(true);
        }

        public BaseStateMachine(String partitionPeerId, int partitionNumber, Options dbOptions) {
                this.partitionPeerId = partitionPeerId;
                this.partitionNumber = partitionNumber;
                this.dbOptions = dbOptions;
        }

        private static Logger logger = LoggerFactory.getLogger(AdminPortalServer.class);

        private DB dbCreate() throws IOException {
                return factory.open(new File("/tmp/leveldbp" + partitionNumber + "id" + partitionPeerId), dbOptions);
        }

        @Override
        public CompletableFuture<Message> query(Message request) {
                try {
                        final String[] opKey = request.getContent().toString(Charset.defaultCharset()).split(":", 2);
                        final String result = opKey[0] + ":" + key2values.get(opKey[1]);

                        System.out.printf("QUERY -> %s: %s = %s\n", opKey[0], opKey[1], result);
                        return CompletableFuture.completedFuture(Message.valueOf(result));
                } catch(Exception exception) {
                        System.out.println("Não foi possível completar a query " + request.toString());
                        return CompletableFuture.failedFuture(exception);
                }
        }


        @Override
        public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
                DB db;
                try {
                        db = dbCreate();
                } catch (IOException e) {
                        return CompletableFuture.failedFuture(e);
                }
                try {
                        final RaftProtos.LogEntryProto entry = trx.getLogEntry();
                        final String[] opKeyValue = entry.getStateMachineLogEntry().getLogData().toString(Charset.defaultCharset()).split(":", 3);

                        final String result = opKeyValue[0] + ":" + key2values.put(opKeyValue[1], opKeyValue[2]);

                        final CompletableFuture<Message> f = CompletableFuture.completedFuture(Message.valueOf(result));

                        final RaftProtos.RaftPeerRole role = trx.getServerRole();
                        System.out.printf("TRANSACTION -> %s:%s %s %s=%s\n", role, getId(), opKeyValue[0], opKeyValue[1], opKeyValue[2]);

                        // escrever pra db leveldb
                        if ("del".equals(opKeyValue[0]) || "null".equals(opKeyValue[2])) {
                                System.out.println("Deletando no LEVELDB " + opKeyValue[1]);
                                db.delete(opKeyValue[1].getBytes());
                        } else {
                                System.out.println("Escrevendo no LEVELDB "  + opKeyValue[1] + " = " + opKeyValue[2]);
                                db.put(opKeyValue[1].getBytes(), opKeyValue[2].getBytes());
                        }

                        return f;
                } catch (Exception exception) {
                        System.out.println("Não foi possível completar a transacao " + trx.getLogEntry().getStateMachineLogEntry().getLogData().toString(Charset.defaultCharset()));
                        return CompletableFuture.failedFuture(exception);
                } finally {
                        try {
                                db.close();
                        } catch (IOException e) {
                                System.err.println("Não foi possível fechar conexão com db:");
                                e.printStackTrace();
                        }
                }
        }
}
