package com.codemacro.kvproxy;

import org.xnio.channels.StreamSinkChannel;

import java.nio.ByteBuffer;

/**
 * Created on 2017/4/9.
 */
public interface ConnectionListener {
  void handleRead(final ByteBuffer buffer, final StreamSinkChannel sink);
}
