package com.codemacro.kvproxy;

import java.util.concurrent.ExecutorService;

/**
 * Created on 2017/4/9.
 */
public interface Service {
  void initialize(ExecutorService executor, Config conf, ServerLocator locator);
  ConnectionListener newListener();
}
