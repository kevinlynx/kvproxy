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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Created on 2017/5/20.
 */
public class RawMemClient implements ChannelListener<ConduitStreamSourceChannel> {
  private final static Logger logger = LoggerFactory.getLogger(RawMemClient.class.getName());
  private final static long AWAIT_TIMEOUT = 10L;
  private final XnioWorker worker;
  private final ConcurrentLinkedQueue<Request> outstandings = new ConcurrentLinkedQueue<>();
  private final LinkedBlockingQueue<AwaitWrite> awaitings = new LinkedBlockingQueue<>();
  private StreamSinkChannel sink = null;
  private StreamConnection conn = null;
  private Response response = null;

  private static class AwaitWrite {
    public Request<?> request;
    public final ByteBuffer buf;

    public AwaitWrite(Request<?> request) {
      buf = request.encode();
      buf.flip();
      this.request = request;
    }
  }

  public RawMemClient(final XnioWorker worker) {
    this.worker = worker;
    this.response = new Response();
  }

  // block until all bytes written
  public <T> FutureResult<T> send(Request<T> req, ExecutorService executor, FutureCallback<T> callback) {
    FutureResult<T> future = req.getFuture();
    future.setCallback(callback, executor);
    AwaitWrite await = new AwaitWrite(req);
    awaitings.offer(await);
    sink.resumeWrites();
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
      this.conn.getSinkChannel().setWriteListener(channel -> {
        try {
          // maybe called all the time because WRITE event happens always
          AwaitWrite await = awaitings.poll(AWAIT_TIMEOUT, TimeUnit.MILLISECONDS);
          if (await == null) return ;
          outstandings.offer(await.request);
          Channels.writeBlocking(channel, await.buf); // maybe not effective cause it will block READ operation
        } catch (InterruptedException e) {
          logger.warn("poll awaiting exception", e);
        } catch (IOException e) {
          logger.warn("write request failed", e);
        }
      });
      sink = future.get().getSinkChannel();
    } catch (IOException e) {
      logger.warn("connect await exception", e);
    }
  }

  @Override
  public void handleEvent(ConduitStreamSourceChannel channel) {
    ByteBuffer buf = ByteBuffer.allocate(4096);
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

