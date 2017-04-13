package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.ConnectionListener;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.Channels;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Created on 2017/4/12.
 */
public class RequestHandler implements ConnectionListener {
  private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class.getName());
  private ProtocolParser parser;
  private final MemcachedClient client;

  public RequestHandler(MemcachedClient client) {
    parser = new ProtocolParser();
    this.client = client;
  }

  public void handleRead(ByteBuffer buffer, final StreamSinkChannel sink) {
    try {
      handleRequest(buffer, sink);
    } catch (ProtocolParser.ParseException e) {
      logger.error("parse exception", e);
      reply("CLIENT_ERROR protocol_failed", sink);
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (ExecutionException e) {
      e.printStackTrace();
    } catch (MemcachedException e) {
      e.printStackTrace();
    } catch (TimeoutException e) {
      e.printStackTrace();
    }
  }

  private void handleRequest(final ByteBuffer buffer, final StreamSinkChannel sink)
      throws ExecutionException, InterruptedException, TimeoutException, MemcachedException {
    while (buffer.hasRemaining()) { // commands after a noreply `set'
      boolean finished = parser.consume(buffer);
      if (!finished) break;
      String cmd = parser.getCommand();
      if (parser.isStoreCmd()) {
        String key = parser.getKeys().get(0);
        byte[] content = parser.cloneData();
        // TODO: pass client flag
        boolean ret = client.set(key, (int) parser.getExpTime(), content);
        if (!parser.isNoreply()) {
          reply(ret ? "STORED" : "NOT_STORED", sink);
        }
      } else if (parser.isRetrieveCmd()) {
        if (cmd.equals("get")) {
          String key = parser.getKeys().get(0);
          byte[] content = client.get(key);
          ByteBuffer dstBuf = ByteBuffer.allocate(1024);
          if (content == null) {
            dstBuf.put("END".getBytes());
          } else {
            String header = String.format("VALUE %s %d %d\r\n", key, 0, content.length);
            dstBuf.put(header.getBytes());
            dstBuf.put(content);
            dstBuf.put("\r\n".getBytes());
            dstBuf.put("END\r\n".getBytes());
          }
          reply(dstBuf, sink);
        } else {
          logger.error("not supported command <{}>", parser.getCommand());
          throw new ProtocolParser.ParseException();
        }
      }
      parser.reset();
    }
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
