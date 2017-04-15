package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.*;
import net.rubyeye.xmemcached.MemcachedClient;
import net.rubyeye.xmemcached.MemcachedClientBuilder;
import net.rubyeye.xmemcached.XMemcachedClientBuilder;
import net.rubyeye.xmemcached.utils.AddrUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created on 2017/4/9.
 */
public class MemcacheService implements Service, ServiceProvider, LocatorListener {
  private static final Logger logger = LoggerFactory.getLogger(MemcacheService.class.getName());
  private MemcachedClient client;

  public MemcacheService() {
  }

  public void initialize(Config conf, ServerLocator locator) {
    logger.info("initialize memcache service");
    locator.setListener(this);
    List<String> servers = locator.getList();
    try {
      MemcachedClientBuilder builder = new XMemcachedClientBuilder(
          AddrUtil.getAddresses(joinServers(servers)));
      builder.setConnectionPoolSize(conf.clientPoolSize);
      client = builder.build();
    } catch (IOException e) {
      logger.error("create memcache client failed", e);
      throw new RuntimeException(e);
    }
  }

  public ConnectionListener newListener() {
    return new RequestHandler(client);
  }

  public Service newService() {
    return this;
  }

  private String joinServers(List<String> servers) {
    StringBuilder sb = new StringBuilder();
    for (String addr : servers) {
      sb.append(' ').append(addr);
    }
    return sb.toString();
  }

  public void addServers(List<String> servers) {
    try {
      String hosts = joinServers(servers);
      logger.info("add servers: {}", hosts);
      client.addServer(hosts);
      logger.info("current servers:{}", client.getServersDescription());
    } catch (IOException e) {
      logger.error("add new servers failed:", e);
    }
  }

  public void removeServers(List<String> servers) {
    String hosts = joinServers(servers);
    logger.info("remove servers:{}", hosts);
    client.removeServer(hosts);
    logger.info("current servers:{}", client.getServersDescription());
  }
}
