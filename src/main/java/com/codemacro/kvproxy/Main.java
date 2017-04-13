package com.codemacro.kvproxy;

import com.codemacro.kvproxy.locator.ConstantLocator;
import com.codemacro.kvproxy.memcache.MemcacheService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created on 2017/4/9.
 */
public class Main {
  public static void main(String[] args) throws IOException {
    Config conf = loadConf();
    MemcacheService memcache = new MemcacheService();
    ServerLocator locator = null;
    if (conf.isConstantLoc()) {
      locator = new ConstantLocator(conf.servers);
    } else {
      throw new RuntimeException("no valid locators found");
    }
    KVProxy proxy = new KVProxy(memcache, locator);
    proxy.start(conf);
  }

  private static Config loadConf() throws IOException {
    File file = new File("kvproxy.conf");
    FileInputStream stream = new FileInputStream(file);
    return Config.build(stream);
  }
}
