package com.codemacro.kvproxy;

import org.xnio.XnioWorker;

/**
 * Created on 2017/4/9.
 */
public interface Service {
  void initialize(XnioWorker worker, Config conf, ServerLocator locator);
  ConnectionListener newListener();
}
