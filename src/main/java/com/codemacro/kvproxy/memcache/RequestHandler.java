package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.ConnectionListener;
import com.codemacro.kvproxy.client.FutureCallback;
import com.codemacro.kvproxy.client.MemcacheStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.Channels;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

/**
 * Created on 2017/4/12.
 */
public class RequestHandler implements ConnectionListener {
  private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class.getName());
  private ProtocolParser parser;
  private final KVClient client;
  private final ExecutorService executor;

  public RequestHandler(ExecutorService executor, KVClient client) {
    this.executor = executor;
    parser = new ProtocolParser();
    this.client = client;
  }

  public void handleRead(ByteBuffer buffer, final StreamSinkChannel sink) {
    try {
      handleRequest(buffer, sink);
    } catch (ProtocolParser.ParseException e) {
      logger.error("parse exception", e);
      reply("CLIENT_ERROR protocol_failed", sink);
    }
  }

  private void handleRequest(final ByteBuffer buffer, final StreamSinkChannel sink) {
    while (buffer.hasRemaining()) { // commands after a noreply `set'
      boolean finished = parser.consume(buffer);
      if (!finished) break;
      try {
        handleRequest0(parser, sink);
      } catch (IOException e) {
        logger.error("handle request exception", e);
      }
      parser.reset();
    }
  }

  private void handleRequest0(final ProtocolParser parser, final StreamSinkChannel sink)
      throws IOException {
    String cmd = parser.getCommand();
    if (parser.isStoreCmd()) {
      processStore(parser, sink);
    } else if (parser.isRetrieveCmd()) {
      if (cmd.equals("get")) {
        final String key = parser.getKey();
        client.asyncGet(key, new FutureCallback<byte[]>() {
          @Override
          public void onSuccess(byte[] bytes) {
            if (bytes != null) {
              ByteBuffer buf = formatGetResp(key, bytes);
              reply(buf, sink);
            } else {
              reply("END", sink);
            }
          }

          @Override
          public void onFailure(Throwable throwable) {
            logger.error("[get] failed", throwable);
            replyError("failure", sink);
          }
        });
      } else {
        replyError("not_impl", sink);
      }
    } else if (cmd.equals("delete")) {
      replyError("not_impl", sink);
    } else if (cmd.equals("quit")) {
      sink.close();
      return;
    } else {
      logger.error("not supported command <{}>", parser.getCommand());
      replyError("not_impl", sink);
    }
  }

  private void processStore(ProtocolParser parser, final StreamSinkChannel sink) {
    String key = parser.getKey();
    String cmd = parser.getCommand();
    byte[] content = parser.cloneData();
    boolean noreply = parser.isNoreply();
    if (cmd.equals("set")) {
      client.asyncSet(key, content, parser.getTime(), new FutureCallback<MemcacheStatus>() {
        @Override
        public void onSuccess(MemcacheStatus memcacheStatus) {
          if (!noreply) {
            boolean ret = memcacheStatus.equals(MemcacheStatus.STORED);
            reply(ret ? "STORED" : "NOT_STORED", sink);
          }
        }

        @Override
        public void onFailure(Throwable throwable) {
          logger.error("[set] failed", throwable);
          replyError("failure", sink);
        }
      });
    } else {
      replyError("not_impl", sink);
    }
  }

  private ByteBuffer formatGetResp(String key, byte[] content) {
    ByteBuffer buf = ByteBuffer.allocate(content.length + key.length() + 32);
    String header = String.format("VALUE %s %d %d\r\n", key, 0, content.length);
    buf.put(header.getBytes());
    buf.put(content);
    buf.put("\r\n".getBytes());
    buf.put("END\r\n".getBytes());
    return buf;
  }

  private void replyError(String msg, final StreamSinkChannel sink) {
    reply("SERVER_ERROR " + msg, sink);
  }

  private void reply(String msg, final StreamSinkChannel sink) {
    ByteBuffer buf = ByteBuffer.allocate(32);
    buf.put(msg.getBytes());
    buf.put("\r\n".getBytes());
    reply(buf, sink);
  }

  private void reply(final ByteBuffer dstBuf, final StreamSinkChannel sink) {
    try {
      dstBuf.flip();
      Channels.writeBlocking(sink, dstBuf);
      Channels.flushBlocking(sink);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

}
