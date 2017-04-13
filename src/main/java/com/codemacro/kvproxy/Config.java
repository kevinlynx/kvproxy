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
  public String[] servers;
  public int ioThreadCount;

  public static Config build(InputStream stream) throws IOException {
    Properties prop = new Properties();
    prop.load(stream);
    Config conf = new Config();
    conf.port = Integer.parseInt(prop.getProperty("port", String.valueOf(11210)));
    conf.locator = prop.getProperty("locator", LOC_CONSTANT);
    if (conf.locator.equals(LOC_CONSTANT)) {
      conf.servers = prop.getProperty("constant.servers", "").split(",");
      if (conf.servers.length == 0) {
        throw new RuntimeException("invalid config, require servers");
      }
    }
    conf.ioThreadCount = Integer.parseInt(prop.getProperty("ioThreadCount"));
    return conf;
  }

  public boolean isConstantLoc() {
    return locator.equals(LOC_CONSTANT);
  }
}
