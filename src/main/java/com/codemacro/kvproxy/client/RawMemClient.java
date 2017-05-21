package com.codemacro.kvproxy.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.*;
import org.xnio.channels.Channels;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.conduits.ConduitStreamSourceChannel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;

/**
 * Created on 2017/5/20.
 */
public class RawMemClient implements ChannelListener<ConduitStreamSourceChannel> {
  private final static Logger logger = LoggerFactory.getLogger(RawMemClient.class.getName());
  private final XnioWorker worker;
  private final ConcurrentLinkedQueue<Request> outstandings = new ConcurrentLinkedQueue<>();
  private StreamSinkChannel sink = null;
  private StreamConnection conn = null;
  private Response response = null;

  public RawMemClient(final XnioWorker worker) {
    this.worker = worker;
    this.response = new Response();
  }

  // block until all bytes written
  public <T> FutureResult<T> send(Request<T> req, ExecutorService executor, FutureCallback<T> callback) {
    FutureResult<T> future = req.getFuture();
    future.setCallback(callback, executor);
    final ByteBuffer buf = req.encode();
    // TODO: write if it's writable
    try {
      outstandings.offer(req);
      buf.flip();
      Channels.writeBlocking(sink, buf);
      Channels.flushBlocking(sink);
    } catch (IOException e) {
      logger.warn("write request failed", e);
    }
    return future;
  }

  public <T> FutureResult<T> send(Request<T> req) {
    return send(req, null, null);
  }

  public void connect(InetSocketAddress addr) {
    IoFuture<StreamConnection> future = worker.openStreamConnection(addr, channel -> {
      if (channel.isOpen()) {
        logger.info("successfully connect to {}", channel.getPeerAddress());
        channel.getSourceChannel().setReadListener(RawMemClient.this);
        channel.getSourceChannel().resumeReads();
      }
    }, OptionMap.EMPTY);
    future.await();
    try {
      this.conn = future.get();
      this.conn.getSourceChannel().setReadListener(RawMemClient.this);
      sink = future.get().getSinkChannel();
    } catch (IOException e) {
      logger.warn("connect await exception", e);
    }
  }

  @Override
  public void handleEvent(ConduitStreamSourceChannel channel) {
    ByteBuffer buf = ByteBuffer.allocate(1024);
    try {
      int res;
      while ((res = channel.read(buf)) > 0) {
      }
      if (res == -1) {
        // TODO: reconnect
        channel.close();
        logger.info("remote server closed connection");
      } else {
        buf.flip();
        while (buf.remaining() > 0) {
          if (response.decode(buf)) {
            Request req = outstandings.poll();
            req.handleResponse(response);
            response = new Response();
          }
        }
        channel.resumeReads();
      }
    } catch (IOException e) {
      logger.warn("read response exception", e);
      try {
        // TODO: reconnect
        channel.close();
      } catch (IOException e1) {
      }
    }
  }
}

