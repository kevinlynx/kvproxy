package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.ConnectionListener;
import com.codemacro.kvproxy.ServerLocator;
import com.codemacro.kvproxy.Service;
import com.codemacro.kvproxy.ServiceProvider;
import net.spy.memcached.AddrUtil;
import net.spy.memcached.MemcachedClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Created on 2017/4/9.
 */
public class MemcacheService implements Service, ServiceProvider {
  private static final Logger logger = LoggerFactory.getLogger(MemcacheService.class.getName());
  private MemcachedClient client;

  public MemcacheService() {
  }

  public void initialize(ServerLocator locator) {
    logger.info("initialize memcache service");
    List<String> servers = locator.getList();
    try {
      client = new MemcachedClient(AddrUtil.getAddresses(servers));
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
}
