package com.codemacro.kvproxy;

import java.util.List;

/**
 * Created on 2017/4/9.
 */
public interface ServerLocator {
  void initialize();
  List<String> getList();
}
