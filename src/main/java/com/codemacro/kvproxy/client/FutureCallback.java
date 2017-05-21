package com.codemacro.kvproxy.client;

/**
 * Created on 2017/5/21.
 */
public interface FutureCallback<T> {
  void onSuccess(T result);
  void onFailure(Throwable t);
}
