package com.codemacro.kvproxy.client;

import org.xnio.XnioWorker;
import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;

/**
 * Created on 2017/5/21.
 */
public class SimpleMemcacheClient implements MemcacheClient {
  private RawMemClient client;
  private ExecutorService executor;

  public SimpleMemcacheClient(XnioWorker worker, ExecutorService executor) {
    this.client = new RawMemClient(worker);
    this.executor = executor;
  }

  public void connect(InetSocketAddress addr) {
    client.connect(addr);
  }

  @Override
  public FutureResult<MemcacheStatus> set(String key, byte[] data, int ttl, FutureCallback<MemcacheStatus> callback) {
    SetRequest req = new SetRequest(key, data, ttl, false);
    return client.send(req, executor, callback);
  }

  @Override
  public FutureResult<byte[]> get(String key, FutureCallback<byte[]> callback) {
    GetRequest req = new GetRequest(key);
    return client.send(req, executor, callback);
  }
}

