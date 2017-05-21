package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.*;
import com.codemacro.kvproxy.client.MemcacheClient;
import com.codemacro.kvproxy.client.MemcacheClientBuilder;
import com.codemacro.kvproxy.locator.GroupLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.XnioWorker;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2017/4/9.
 */
public class MemcacheService implements Service, ServiceProvider, GroupClient.ClientCreator {
  private static final Logger logger = LoggerFactory.getLogger(MemcacheService.class.getName());
  private KVClient client;
  private XnioWorker worker;

  public void initialize(XnioWorker worker, Config conf, ServerLocator locator) {
    logger.info("initialize memcache service");
    this.worker = worker;
    if (!(locator instanceof GroupLocator)) {
      throw new RuntimeException("require group locator");
    } // OR we can create a single client
    GroupClient groupClient = new GroupClient(worker);
    groupClient.initialize(conf, (GroupLocator) locator, this);
    client = groupClient;
  }

  public ConnectionListener newListener() {
    return new RequestHandler(worker, client);
  }

  public Service newService() {
    return this;
  }

  @Override
  public KVClient create(Config conf, ServerLocator locator) {
    List<String> servers = locator.getList();
    MemcacheClientBuilder builder = new MemcacheClientBuilder();
    final MemcacheClient client = builder.setAddrs(parseServers(servers)).setWorker(worker).build();
    KVClient kvclient = new MemClient(client, worker);
    return kvclient;
  }

  private List<InetSocketAddress> parseServers(List<String> servers) {
    List<InetSocketAddress> addrs = new ArrayList<>();
    for (String s : servers) {
      String[] vec = s.split(":");
      addrs.add(new InetSocketAddress(vec[0], Integer.parseInt(vec[1])));
    }
    return addrs;
  }
}
