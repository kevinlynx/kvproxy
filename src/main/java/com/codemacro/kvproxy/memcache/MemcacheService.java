package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.*;
import com.codemacro.kvproxy.locator.GroupLocator;
import com.google.common.net.HostAndPort;
import com.spotify.folsom.ConnectFuture;
import com.spotify.folsom.MemcacheClient;
import com.spotify.folsom.MemcacheClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;

/**
 * Created on 2017/4/9.
 */
public class MemcacheService implements Service, ServiceProvider, GroupClient.ClientCreator {
  private static final Logger logger = LoggerFactory.getLogger(MemcacheService.class.getName());
  private KVClient client;
  private ExecutorService executor;

  public void initialize(ExecutorService executor, Config conf, ServerLocator locator) {
    logger.info("initialize memcache service");
    this.executor = executor;
    if (!(locator instanceof GroupLocator)) {
      throw new RuntimeException("require group locator");
    } // OR we can create a single client
    GroupClient groupClient = new GroupClient(executor);
    groupClient.initialize(conf, (GroupLocator) locator, this);
    client = groupClient;
  }

  public ConnectionListener newListener() {
    return new RequestHandler(executor, client);
  }

  public Service newService() {
    return this;
  }

  @Override
  public KVClient create(Config conf, ServerLocator locator) {
    try {
      List<String> servers = locator.getList();
      final MemcacheClient<byte[]> client = MemcacheClientBuilder.newByteArrayClient()
          .withAddresses(parseServers(servers))
          .withRequestTimeoutMillis(conf.clientOpTimeout)
          .withConnections(conf.clientPoolSize)
          .connectAscii();
      ConnectFuture.connectFuture(client).get();
      KVClient kvclient = new MemClient(client, executor);
      return kvclient;
    } catch (InterruptedException e) {
      logger.error("create client failed", e);
    } catch (ExecutionException e) {
      logger.error("create client failed", e);
    }
    return null;
  }

  private List<HostAndPort> parseServers(List<String> servers) {
    List<HostAndPort> addrs = new ArrayList<>();
    for (String s : servers) {
      addrs.add(HostAndPort.fromString(s));
    }
    return addrs;
  }
}
