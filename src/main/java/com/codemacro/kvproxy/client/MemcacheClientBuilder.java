package com.codemacro.kvproxy.client;

import org.xnio.XnioWorker;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Created on 2017/5/21.
 */
public class MemcacheClientBuilder {
  private List<InetSocketAddress> addrs = new ArrayList<>();
  private XnioWorker worker;

  public MemcacheClientBuilder setAddrs(List<InetSocketAddress> addrs) {
    this.addrs = addrs;
    return this;
  }

  public MemcacheClientBuilder setWorker(XnioWorker worker) {
    this.worker = worker;
    return this;
  }

  public MemcacheClient build() {
    SimpleMemcacheClient client = new SimpleMemcacheClient(worker, worker);
    client.connect(addrs.get(0));
    return client;
  }
}
