package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.client.FutureCallback;
import com.codemacro.kvproxy.client.MemcacheStatus;

/**
 * Created on 2017/4/16.
 */
public interface KVClient {
  void addServers(String hosts);
  void removeServers(String hosts);

  void asyncGet(String key, FutureCallback<byte[]> callback);
  void asyncSet(String key, byte[] content, int ttl, FutureCallback<MemcacheStatus> callback);
  void asyncDelete(String key, FutureCallback<MemcacheStatus> callback);
}
