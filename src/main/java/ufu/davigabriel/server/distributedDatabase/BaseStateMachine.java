package ufu.davigabriel.server.distributedDatabase;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.ratis.proto.*;
import org.apache.ratis.protocol.Message;
import org.apache.ratis.statemachine.TransactionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ufu.davigabriel.server.AdminPortalServer;


public class BaseStateMachine extends org.apache.ratis.statemachine.impl.BaseStateMachine {
        private final Map<String, String> key2values = new ConcurrentHashMap<>();

        private static Logger logger = LoggerFactory.getLogger(AdminPortalServer.class);

        @Override
        public CompletableFuture<Message> query(Message request) {
                try {
                        final String[] opKey = request.getContent().toString(Charset.defaultCharset()).split(":");
                        final String result = opKey[0] + ":" + key2values.get(opKey[1]);

                        LOG.debug("{}: {} = {}", opKey[0], opKey[1], result);
                        return CompletableFuture.completedFuture(Message.valueOf(result));
                } catch(Exception exception) {
                        return CompletableFuture.failedFuture(exception);
                }
        }

        @Override
        public CompletableFuture<Message> applyTransaction(TransactionContext trx) {
                try {
                        LOG.info(trx.toString());
                        final RaftProtos.LogEntryProto entry = trx.getLogEntry();
                        final String[] opKeyValue = entry.getStateMachineLogEntry().getLogData().toString(Charset.defaultCharset()).split(":");

                        LOG.info(Arrays.toString(opKeyValue));
                        final String result = opKeyValue[0] + ":" + key2values.put(opKeyValue[1], opKeyValue[2]);

                        final CompletableFuture<Message> f = CompletableFuture.completedFuture(Message.valueOf(result));

                        final RaftProtos.RaftPeerRole role = trx.getServerRole();
                        LOG.info("{}:{} {} {}={}", role, getId(), opKeyValue[0], opKeyValue[1], opKeyValue[2]);

                        return f;
                } catch (Exception exception) {
                        return CompletableFuture.failedFuture(exception);
                }
        }
}
