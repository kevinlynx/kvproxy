package com.codemacro.kvproxy.client;

import java.nio.ByteBuffer;

/**
 * Created on 2017/5/21.
 */
public class GetRequest extends Request<byte[]> {
  private String key;

  public GetRequest(String key) {
    this.key = key;
  }

  @Override
  public ByteBuffer encode() {
    ByteBuffer buf = ByteBuffer.allocate(key.length() + 32);
    buf.put("get ".getBytes());
    buf.put(key.getBytes());
    buf.put("\r\n".getBytes());
    return buf;
  }

  @Override
  public void handleResponse(Response resp) {
    future.set(resp.getValue(key));
  }
}
