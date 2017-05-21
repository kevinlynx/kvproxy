package com.codemacro.kvproxy.client;

/**
 * Created on 2017/5/21.
 */
public interface MemcacheClient {
  FutureResult<MemcacheStatus> set(String key, byte[] data, int ttl, FutureCallback<MemcacheStatus> callback);
  FutureResult<byte[]> get(String key, FutureCallback<byte[]> callback);
}
