package com.codemacro.kvproxy;

import com.codemacro.kvproxy.memcache.MemcacheService;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Created on 2017/4/9.
 */
public class App {
  private ServerLocator locator;
  private Config conf;

  public App() {
  }

  public void start() throws IOException {
    conf = loadConf();
    ServiceProvider provider = new MemcacheService();
    locator = loadLocator(conf);
    KVProxy proxy = new KVProxy(provider, locator);
    proxy.start(conf);
  }

  private Config loadConf() throws IOException {
    File file = new File("kvproxy.conf");
    FileInputStream stream = new FileInputStream(file);
    return Config.build(stream);
  }

  private ServerLocator loadLocator(Config conf) {
    try {
      Class clazz = Class.forName(conf.locator);
      ServerLocator locator = (ServerLocator) clazz.newInstance();
      locator.initialize(conf.locatorConf);
      return locator;
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) throws IOException {
    App app = new App();
    app.start();
  }
}
