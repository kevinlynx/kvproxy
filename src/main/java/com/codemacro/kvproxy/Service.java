package com.codemacro.kvproxy;

/**
 * Created on 2017/4/9.
 */
public interface Service {
  void initialize(Config conf, ServerLocator locator);
  ConnectionListener newListener();
}