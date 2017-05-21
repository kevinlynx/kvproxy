package com.codemacro.kvproxy.memcache;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * Created on 2017/4/16.
 */
public class MemClient implements KVClient {
  private static final Logger logger = LoggerFactory.getLogger(MemClient.class.getName());
  private MemcacheClient<byte[]> client;
  private ExecutorService executor;

  public MemClient(MemcacheClient client, final ExecutorService executor) {
    this.client = client;
    this.executor = executor;
  }

  public void addServers(String hosts) {
    logger.warn("not supported dynamically add servers");
  }

  public void removeServers(String hosts) {
    logger.warn("not supported dynamically remove servers");
  }

  @Override
  public void asyncGet(String key, FutureCallback<byte[]> callback) {
    Futures.addCallback(client.get(key), callback);
  }

  @Override
  public void asyncSet(String key, byte[] content, int ttl, FutureCallback<MemcacheStatus> callback) {
    Futures.addCallback(client.set(key, content, ttl), callback);
  }

  @Override
  public void asyncDelete(String key, FutureCallback<MemcacheStatus> callback) {
    Futures.addCallback(client.delete(key), callback);
  }
}
