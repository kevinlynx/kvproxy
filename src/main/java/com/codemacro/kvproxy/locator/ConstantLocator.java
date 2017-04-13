package com.codemacro.kvproxy.locator;

import com.codemacro.kvproxy.ServerLocator;
import java.util.Arrays;
import java.util.List;

/**
 * Created on 2017/4/9.
 */
public class ConstantLocator implements ServerLocator {
  private final String[] servers;

  public ConstantLocator(String[] servers) {
    this.servers = servers;
  }

  public void initialize() {
  }

  public List<String> getList() {
    return Arrays.asList(servers);
  }
}
