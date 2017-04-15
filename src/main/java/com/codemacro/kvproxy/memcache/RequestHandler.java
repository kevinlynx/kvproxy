package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.ConnectionListener;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import net.rubyeye.xmemcached.utils.Protocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.channels.Channels;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
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
        processStore(parser, sink);
      } else if (parser.isRetrieveCmd()) {
        ByteBuffer buf = getStoredData(parser);
        reply(buf, sink);
      } else if (cmd.equals("delete")) {
        if (parser.isNoreply()) {
          client.deleteWithNoReply(parser.getKey());
        } else {
          boolean ret = client.delete(parser.getKey(), parser.getTime());
          reply(ret ? "DELETED" : "NOT_FOUND", sink);
        }
      } else if (parser.isIncDecCmd()) {
        long ret;
        if (cmd.equals("incr")) {
          ret = client.incr(parser.getKey(), parser.getValue());
          // TODO: what if not exist ?
        } else {
          ret = client.decr(parser.getKey(), parser.getValue());
        }
        if (!parser.isNoreply()) {
          reply(String.valueOf(ret), sink);
        }
      } else if (cmd.equals("flush_all")) {
        if (parser.isNoreply()) {
          client.flushAllWithNoReply();
        } else {
          client.flushAll();
          reply("OK", sink);
        }
      } else {
        logger.error("not supported command <{}>", parser.getCommand());
        throw new ProtocolParser.ParseException();
      }
      parser.reset();
    }
  }

  private void processStore(ProtocolParser parser, final StreamSinkChannel sink)
      throws InterruptedException, MemcachedException, TimeoutException {
    String key = parser.getKey();
    String cmd = parser.getCommand();
    byte[] content = parser.cloneData();
    boolean ret = true;
    boolean noreply = parser.isNoreply();
    if (cmd.equals("set")) {
      if (noreply) {
        client.setWithNoReply(key, parser.getTime(), content);
      } else {
        ret = client.set(key, parser.getTime(), content);
      }
    } else if (cmd.equals("add")) {
      if (noreply) {
        client.addWithNoReply(key, parser.getTime(), content);
      } else {
        ret = client.add(key, parser.getTime(), content);
      }
    } else if (cmd.equals("append")) {
      if (noreply) {
        client.appendWithNoReply(key, content);
      } else {
        ret = client.append(key, content);
      }
    } else if (cmd.equals("prepend")) {
      if (noreply) {
        client.prependWithNoReply(key, content);
      } else {
        ret = client.prepend(key, content);
      }
    }
    if (!noreply) {
      reply(ret ? "STORED" : "NOT_STORED", sink);
    }
  }

  private ByteBuffer getStoredData(ProtocolParser parser)
      throws InterruptedException, MemcachedException, TimeoutException {
    List<String> keys = parser.getKeys();
    Map<String, byte[]> results = client.get(keys);
    int size = 5; // END\r\n
    for (Map.Entry<String, byte[]> entry : results.entrySet()) {
      String key = entry.getKey();
      byte[] content = entry.getValue();
      size += content.length;
      size += (key.length() + 32);
    }
    ByteBuffer buf = ByteBuffer.allocate(size);
    for (Map.Entry<String, byte[]> entry : results.entrySet()) {
      String key = entry.getKey();
      byte[] content = entry.getValue();
      String header = String.format("VALUE %s %d %d\r\n", key, 0, content.length);
      buf.put(header.getBytes());
      buf.put(content);
      buf.put("\r\n".getBytes());
    }
    buf.put("END\r\n".getBytes());
    return buf;
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
