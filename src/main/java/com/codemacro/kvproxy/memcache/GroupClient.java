package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.Config;
import com.codemacro.kvproxy.LocatorListener;
import com.codemacro.kvproxy.ServerLocator;
import com.codemacro.kvproxy.locator.GroupLocator;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 2017/4/16.
 */
public class GroupClient implements KVClient {
  private static final Logger logger = LoggerFactory.getLogger(GroupClient.class.getName());
  private static final String PRIMARY_NAME = "primary";
  private KVClient primary = null;
  private Map<String, KVClient> slaves = new HashMap<String, KVClient>();

  private class ClientListener implements LocatorListener {
    private KVClient client;

    public ClientListener(KVClient client) {
      this.client = client;
    }

    public void addServers(List<String> servers) {
      client.addServers(joinServers(servers));
    }

    public void removeServers(List<String> servers) {
      client.removeServers(joinServers(servers));
    }
  }

  public void initialize(Config conf, GroupLocator locator) {
    primary = createClient(conf, PRIMARY_NAME, locator);
    if (conf.groupClientConf == null) {
      throw new RuntimeException("require group client conf");
    }
    for (String name : conf.groupClientConf.split(",")) {
      KVClient client = createClient(conf, name, locator);
      slaves.put(name, client);
    }
  }

  public void addServers(String hosts) {

  }

  public void removeServers(String hosts) {

  }

  public void deleteWithNoReply(String key) {
    primary.deleteWithNoReply(key);
    for (KVClient client : slaves.values()) {
      client.deleteWithNoReply(key);
    }
  }

  public boolean delete(String key, int time) {
    boolean ret = primary.delete(key, time);
    for (KVClient client : slaves.values()) {
      client.deleteWithNoReply(key);
    }
    return ret;
  }

  public long incr(String key, long inc) {
    long ret = primary.incr(key, inc);
    for (KVClient client : slaves.values()) {
      client.incr(key, inc);
    }
    return ret;
  }

  public long decr(String key, long dec) {
    long ret = primary.decr(key, dec);
    for (KVClient client : slaves.values()) {
      client.decr(key, dec);
    }
    return ret;
  }

  public void flushAllWithNoReply() {
    primary.flushAllWithNoReply();
    for (KVClient client : slaves.values()) {
      client.flushAllWithNoReply();
    }
  }

  public void flushAll() {
    primary.flushAll();
    for (KVClient client : slaves.values()) {
      client.flushAll();
    }
  }

  public void setWithNoReply(String key, int time, byte[] content) {
    primary.setWithNoReply(key, time, content);
    for (KVClient client : slaves.values()) {
      client.setWithNoReply(key, time, content);
    }
  }

  public boolean set(String key, int time, byte[] content) {
    boolean ret = primary.set(key, time, content);
    for (KVClient client : slaves.values()) {
      client.setWithNoReply(key, time, content);
    }
    return ret;
  }

  public void addWithNoReply(String key, int time, byte[] content) {
    primary.addWithNoReply(key, time, content);
    for (KVClient client : slaves.values()) {
      client.addWithNoReply(key, time, content);
    }
  }

  public boolean add(String key, int time, byte[] content) {
    boolean ret = primary.add(key, time, content);
    for (KVClient client : slaves.values()) {
      client.add(key, time, content);
    }
    return ret;
  }

  public void appendWithNoReply(String key, byte[] content) {
    primary.appendWithNoReply(key, content);
    for (KVClient client : slaves.values()) {
      client.appendWithNoReply(key, content);
    }
  }

  public boolean append(String key, byte[] content) {
    boolean ret = primary.append(key, content);
    for (KVClient client : slaves.values()) {
      client.append(key, content);
    }
    return ret;
  }

  public void prependWithNoReply(String key, byte[] content) {
    primary.prependWithNoReply(key, content);
    for (KVClient client : slaves.values()) {
      client.prependWithNoReply(key, content);
    }
  }

  public boolean prepend(String key, byte[] content) {
    boolean ret = primary.prepend(key, content);
    for (KVClient client : slaves.values()) {
      client.prepend(key, content);
    }
    return ret;
  }

  public Map<String, byte[]> get(Collection<String> keys) {
    return primary.get(keys);
  }

  private KVClient createClient(Config conf, String name, GroupLocator locator) {
    ServerLocator childLoc = locator.getLocator(name);
    if (childLoc == null) {
      throw new RuntimeException("not found locator for " + name);
    }
    return createClient(conf, childLoc);
  }

  // create a client associate by this locator
  private KVClient createClient(Config conf, ServerLocator locator) {
    try {
      List<String> servers = locator.getList();
      MemcachedClientBuilder builder = new XMemcachedClientBuilder(
          AddrUtil.getAddresses(joinServers(servers)));
      builder.setConnectionPoolSize(conf.clientPoolSize);
      KVClient client = new MemClient(builder.build());
      locator.setListener(new ClientListener(client));
      return client;
    } catch (IOException e) {
      logger.error("create memcache client failed", e);
      throw new RuntimeException(e);
    }
  }

  private String joinServers(List<String> servers) {
    StringBuilder sb = new StringBuilder();
    for (String addr : servers) {
      sb.append(' ').append(addr);
    }
    return sb.toString();
  }
}
