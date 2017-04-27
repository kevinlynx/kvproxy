package com.codemacro.kvproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.StreamConnection;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;

/**
 * Created on 2017/4/9.
 */
public class Connection implements ChannelListener<StreamSourceChannel> {
  private static final Logger logger = LoggerFactory.getLogger(Connection.class.getName());
  private final StreamConnection stream;
  private final ConnectionListener listener;
  private final ExecutorService executor;

  public Connection(ExecutorService executor, StreamConnection stream, final ConnectionListener listener) {
    this.executor = executor;
    this.stream = stream;
    this.listener = listener;
  }

  public void initialize() {
    stream.getSourceChannel().setReadListener(this);
    stream.getSourceChannel().resumeReads();
  }

  public void handleEvent(StreamSourceChannel channel) {
    final ByteBuffer buffer = ByteBuffer.allocate(1024);
    int res;
    try {
      while ((res = channel.read(buffer)) > 0) {
      }
      if (res == -1) {
        channel.close();
      } else {
        buffer.flip();
        if (buffer.remaining() > 0) {
          listener.handleRead(buffer, stream.getSinkChannel());
        } else {
          logger.warn("read empty buffer for {}", stream.getPeerAddress());
        }
        channel.resumeReads();
      }
    } catch (IOException e) {
      logger.warn("read channel exception:", e);
      e.printStackTrace();
      try {
        channel.close();
      } catch (IOException e1) {
        e1.printStackTrace();
      }
    }
  }
}

