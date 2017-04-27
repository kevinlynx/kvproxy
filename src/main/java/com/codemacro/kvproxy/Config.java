package com.codemacro.kvproxy;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created on 2017/4/12.
 */
public class Config {
  private static final String LOC_CONSTANT = "constant";
  public int port;
  public String locator;
  public String locatorConf;
  public int ioThreadCount;
  public int clientPoolSize;
  public int clientOpTimeout;
  public String groupClientConf;

  public static Config build(InputStream stream) throws IOException {
    Properties prop = new Properties();
    prop.load(stream);
    Config conf = new Config();
    conf.port = Integer.parseInt(prop.getProperty("port", String.valueOf(11210)));
    conf.locator = prop.getProperty("locator", LOC_CONSTANT);
    conf.locatorConf = prop.getProperty("locatorConf", "");
    conf.ioThreadCount = Integer.parseInt(prop.getProperty("ioThreadCount"));
    conf.clientPoolSize = Integer.parseInt(prop.getProperty("clientPoolSize", String.valueOf(1)));
    conf.clientOpTimeout = Integer.parseInt(prop.getProperty("clientOpTimeout", String.valueOf(3000)));
    conf.groupClientConf = prop.getProperty("groupClientConf", "");
    return conf;
  }
}
