package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.Config;
import com.codemacro.kvproxy.LocatorListener;
import com.codemacro.kvproxy.ServerLocator;
import com.codemacro.kvproxy.client.FutureCallback;
import com.codemacro.kvproxy.client.MemcacheStatus;
import com.codemacro.kvproxy.locator.GroupLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

/**
 * Created on 2017/4/16.
 */
public class GroupClient implements KVClient {
  private static final Logger logger = LoggerFactory.getLogger(GroupClient.class.getName());
  private static final String PRIMARY_NAME = "primary";
  private KVClient primary = null;
  private Map<String, KVClient> slaves = new HashMap<String, KVClient>();
  private final ExecutorService executor;

  interface ClientCreator {
    KVClient create(Config conf, ServerLocator locator);
  }
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

  public void initialize(Config conf, GroupLocator locator, ClientCreator creator) {
    primary = createClient(conf, PRIMARY_NAME, locator, creator);
    if (conf.groupClientConf == null) {
      throw new RuntimeException("require group client conf");
    }
    if (!conf.groupClientConf.isEmpty()) {
      for (String name : conf.groupClientConf.split(",")) {
        KVClient client = createClient(conf, name.trim(), locator, creator);
        logger.info("add slave {}", name);
        slaves.put(name, client);
      }
    }
  }

  public GroupClient(final ExecutorService executor) {
    this.executor = executor;
  }

  public void addServers(String hosts) {
  }

  public void removeServers(String hosts) {
  }

  @Override
  public void asyncGet(String key, FutureCallback<byte[]> callback) {
    primary.asyncGet(key, callback);
  }

  @Override
  public void asyncSet(String key, byte[] content, int ttl, FutureCallback<MemcacheStatus> callback) {
    primary.asyncSet(key, content, ttl, callback);
  }

  @Override
  public void asyncDelete(String key, FutureCallback<MemcacheStatus> callback) {
    primary.asyncDelete(key, callback);
  }

  private KVClient createClient(Config conf, String name, GroupLocator locator, ClientCreator creator) {
    ServerLocator childLoc = locator.getLocator(name);
    if (childLoc == null) {
      throw new RuntimeException("not found locator for " + name);
    }
    KVClient client = creator.create(conf, childLoc);
    childLoc.setListener(new ClientListener(client));
    return client;
  }

  // create a client associate by this locator
  private String joinServers(List<String> servers) {
    StringBuilder sb = new StringBuilder();
    for (String addr : servers) {
      sb.append(' ').append(addr);
    }
    return sb.toString();
  }
}
