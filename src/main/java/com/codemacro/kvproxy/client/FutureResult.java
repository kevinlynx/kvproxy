package com.codemacro.kvproxy.client;

import java.util.concurrent.*;

/**
 * Created on 2017/5/21.
 */
public class FutureResult<T> implements Future<T>, Runnable {
  private enum Status {
    WAITING, DONE, FAILED
  }
  private T result = null;
  private Status status = Status.WAITING;
  private FutureCallback<T> callback;
  private ExecutorService executor;

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    // TODO:
    return false;
  }

  @Override
  public boolean isCancelled() {
    return false;
  }

  @Override
  public boolean isDone() {
    synchronized (this) {
      return status == Status.DONE || status == Status.FAILED;
    }
  }

  @Override
  public T get() throws InterruptedException, ExecutionException {
    synchronized (this) {
      while (status == Status.WAITING) {
        this.wait();
      }
    }
    return result;
  }

  // TODO: test this
  @Override
  public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
    long duration = unit.toNanos(timeout);
    long waitTime;
    synchronized (this) {
      while (status == Status.WAITING && (waitTime = duration / 1000000L) > 0L) {
        this.wait(waitTime);
      }
    }
    return result;
  }

  @Override
  public void run() {
    // TODO: handle failure
    callback.onSuccess(result);
  }

  public void set(T result) {
    synchronized (this) {
      this.result = result;
      this.status = Status.DONE;
      this.notifyAll();
    }
    if (callback != null) {
      executor.execute(this);
    }
  }

  public void setCallback(FutureCallback<T> callback, ExecutorService executor) {
    this.callback = callback;
    this.executor = executor;
  }
}

