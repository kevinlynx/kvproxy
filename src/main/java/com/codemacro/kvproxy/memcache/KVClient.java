package com.codemacro.kvproxy.memcache;

import net.rubyeye.xmemcached.exception.MemcachedException;

import java.util.Collection;
import java.util.Map;

/**
 * Created on 2017/4/16.
 */
public interface KVClient {
  void addServers(String hosts);
  void removeServers(String hosts);
  void deleteWithNoReply(String key);
  boolean delete(String key, int time);
  long incr(String key, long inc);
  long decr(String key, long dec);
  void flushAllWithNoReply();
  void flushAll();
  void setWithNoReply(String key, int time, byte[] content);
  boolean set(String key, int time, byte[] content);
  void addWithNoReply(String key, int time, byte[] content);
  boolean add(String key, int time, byte[] content);
  void appendWithNoReply(String key, byte[] content);
  boolean append(String key, byte[] content);
  void prependWithNoReply(String key, byte[] content);
  boolean prepend(String key, byte[] content);
  Map<String, byte[]> get(Collection<String> keys);
}
