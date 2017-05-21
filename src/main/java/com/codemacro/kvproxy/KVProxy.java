package com.codemacro.kvproxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.*;
import org.xnio.channels.AcceptingChannel;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * Created on 2017/4/9.
 */
public class KVProxy implements ChannelListener<AcceptingChannel<StreamConnection>> {
  private static final Logger logger = LoggerFactory.getLogger(KVProxy.class.getName());
  private final ServiceProvider provider;
  private XnioWorker worker;
  private final ServerLocator locator;
  private AcceptingChannel<? extends StreamConnection> server;
  private Service service;

  public KVProxy(ServiceProvider provider, ServerLocator locator) throws IOException {
    this.provider = provider;
    this.locator = locator;
    service = provider.newService();
  }

  public boolean start(Config conf) throws IOException {
    OptionMap options = OptionMap.builder().set(Options.WORKER_IO_THREADS, conf.ioThreadCount).getMap();
    worker = Xnio.getInstance().createWorker(options);
    service.initialize(worker, conf, locator);
    server = worker.createStreamConnectionServer(new InetSocketAddress(conf.port), this, OptionMap.EMPTY);
    server.resumeAccepts();
    return true;
  }

  @Override
  public void handleEvent(AcceptingChannel<StreamConnection> channel) {
    try {
      StreamConnection accepted;
      while ((accepted = channel.accept()) != null) {
        logger.debug("accept a new connection: " + accepted.getPeerAddress());
        Connection conn = new Connection(worker, accepted, service.newListener());
        conn.initialize();
      }
    } catch (IOException ignored) {
    }
  }
}
