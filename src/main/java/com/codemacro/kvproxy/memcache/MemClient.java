package com.codemacro.kvproxy.memcache;

import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.exception.MemcachedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Created on 2017/4/16.
 */
public class MemClient implements KVClient {
  private static final Logger logger = LoggerFactory.getLogger(MemClient.class.getName());
  private MemcachedClient client;

  public MemClient(MemcachedClient client) {
    this.client = client;
  }

  public void addServers(String hosts) {
    try {
      client.addServer(hosts);
    } catch (IOException e) {
      logger.error("add new servers failed:", e);
    }
  }

  public void removeServers(String hosts) {
    client.removeServer(hosts);
  }

  interface InvokeVoid {
    void invoke() throws InterruptedException, MemcachedException;
  }

  interface InvokeRet<T> {
    T invoke() throws InterruptedException, TimeoutException, MemcachedException;
  }

  private void invoke(InvokeVoid f) {
    try {
      f.invoke();
    } catch (InterruptedException e) {
      logger.error("memcache exception:", e);
    } catch (MemcachedException e) {
      logger.error("memcache exception:", e);
    }
  }

  private <T> T invoke(InvokeRet<T> f, T def) {
    try {
      return f.invoke();
    } catch (InterruptedException e) {
      logger.error("memcache interrupt:", e);
    } catch (MemcachedException e) {
      logger.error("memcache exception:", e);
    } catch (TimeoutException e) { // TODO: throw this to request handler, response error to client
      logger.error("memcache timeout:", e);
    }
    return def;
  }

  public void deleteWithNoReply(String key) {
    invoke(() -> client.deleteWithNoReply(key));
  }

  public boolean delete(String key, int time) {
    return invoke(() -> client.delete(key, time), false);
  }

  public long incr(String key, long inc) {
    return invoke(() -> client.incr(key, inc), 0L);
  }

  public long decr(String key, long dec) {
    return invoke(() -> client.decr(key, dec), 0L);
  }

  public void flushAllWithNoReply() {
    invoke(() -> client.flushAllWithNoReply());
  }

  public void flushAll() {
    invoke(() -> {
      client.flushAll();
      return true;
    }, true);
  }

  public void setWithNoReply(String key, int time, byte[] content) {
    invoke(() -> client.setWithNoReply(key, time, content));
  }

  public boolean set(String key, int time, byte[] content) {
    return invoke(() -> client.set(key, time, content), false);
  }

  public void addWithNoReply(String key, int time, byte[] content) {
    invoke(() -> client.addWithNoReply(key, time, content));
  }

  public boolean add(String key, int time, byte[] content) {
    return invoke(() -> client.add(key, time, content), false);
  }

  public void appendWithNoReply(String key, byte[] content) {
    invoke(() -> client.appendWithNoReply(key, content));
  }

  public boolean append(String key, byte[] content) {
    return invoke(() -> client.append(key, content), false);
  }

  public void prependWithNoReply(String key, byte[] content) {
    invoke(() -> client.prependWithNoReply(key, content));
  }

  public boolean prepend(String key, byte[] content) {
    return invoke(() -> client.prepend(key, content), false);
  }

  public Map<String, byte[]> get(Collection<String> keys) {
    return invoke(() -> client.get(keys), new HashMap<String, byte[]>());
  }
}
