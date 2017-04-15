package com.codemacro.kvproxy.locator;

import com.codemacro.kvproxy.LocatorListener;
import com.codemacro.kvproxy.ServerLocator;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 2017/4/9.
 */
public class ConstantLocator implements ServerLocator {
  private String[] servers;

  public ConstantLocator() {
  }

  public void initialize(String conf) {
    servers = conf.split(",");
    if (servers.length == 0) {
      throw new RuntimeException("no servers config");
    }
  }

  public void setListener(LocatorListener listener) {
  }

  public List<String> getList() {
    return Arrays.asList(servers);
  }
}
