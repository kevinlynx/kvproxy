package com.codemacro.kvproxy.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;

/**
 * Created on 2017/5/20.
 */
public class SetRequest extends Request<MemcacheStatus> {
  private final static Logger logger = LoggerFactory.getLogger(SetRequest.class.getName());
  private String key;
  private byte[] content;
  private int ttl;
  private boolean noreply;

  public SetRequest(String key, byte[] content, int ttl, boolean noreply) {
    this.key = key;
    this.content = content;
    this.ttl = ttl;
    this.noreply = noreply;
  }

  @Override
  public ByteBuffer encode() {
    ByteBuffer buf = ByteBuffer.allocate(content.length + key.length() +32);
    String header = String.format("set %s 0 %d %d%s\r\n", key, ttl, content.length,
        noreply ? " noreply" : "");
    buf.put(header.getBytes());
    buf.put(content);
    buf.put("\r\n".getBytes());
    return buf;
  }

  @Override
  public void handleResponse(Response resp) {
    if (!resp.getStatus().equals(MemcacheStatus.STORED)) {
      logger.warn("set request not STORED: {}", resp.getMsg());
    }
    future.set(resp.getStatus());
  }
}
