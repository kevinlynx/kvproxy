package com.codemacro.kvproxy;

import java.util.List;

/**
 * Created on 2017/4/15.
 */
public interface LocatorListener {
  void addServers(List<String> servers);
  void removeServers(List<String> servers);
}
