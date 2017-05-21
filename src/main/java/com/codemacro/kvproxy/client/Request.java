package com.codemacro.kvproxy.client;

import java.nio.ByteBuffer;

/**
 * Created on 2017/5/20.
 */
public abstract class Request<T> {
  protected FutureResult<T> future = new FutureResult<T>();

  public FutureResult<T> getFuture() { return future; }
  public abstract ByteBuffer encode();
  public abstract void handleResponse(final Response resp);
}
