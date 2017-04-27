package com.codemacro.kvproxy.memcache;

import com.codemacro.kvproxy.*;
import com.codemacro.kvproxy.locator.GroupLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;

/**
 * Created on 2017/4/9.
 */
public class MemcacheService implements Service, ServiceProvider {
  private static final Logger logger = LoggerFactory.getLogger(MemcacheService.class.getName());
  private KVClient client;
  private ExecutorService executor;

  public MemcacheService() {
  }

  public void initialize(ExecutorService executor, Config conf, ServerLocator locator) {
    logger.info("initialize memcache service");
    this.executor = executor;
    if (!(locator instanceof GroupLocator)) {
      throw new RuntimeException("require group locator");
    } // OR we can create a single client
    GroupClient groupClient = new GroupClient();
    groupClient.initialize(conf, (GroupLocator) locator);
    client = groupClient;
  }

  public ConnectionListener newListener() {
    return new RequestHandler(executor, client);
  }

  public Service newService() {
    return this;
  }
}
