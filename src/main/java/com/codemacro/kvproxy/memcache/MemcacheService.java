package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.*;
import com.codemacro.kvproxy.locator.GroupLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created on 2017/4/9.
 */
public class MemcacheService implements Service, ServiceProvider {
  private static final Logger logger = LoggerFactory.getLogger(MemcacheService.class.getName());
  private KVClient client;

  public MemcacheService() {
  }

  public void initialize(Config conf, ServerLocator locator) {
    logger.info("initialize memcache service");
    if (!(locator instanceof GroupLocator)) {
      throw new RuntimeException("require group locator");
    } // OR we can create a single client
    GroupClient groupClient = new GroupClient();
    groupClient.initialize(conf, (GroupLocator) locator);
    client = groupClient;
  }

  public ConnectionListener newListener() {
    return new RequestHandler(client);
  }

  public Service newService() {
    return this;
  }
}
