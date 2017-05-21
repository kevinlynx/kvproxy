package com.codemacro.kvproxy.memcache;

import com.google.common.util.concurrent.FutureCallback;
import com.spotify.folsom.MemcacheStatus;

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
