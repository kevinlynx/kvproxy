package com.codemacro.kvproxy.locator;

import com.codemacro.kvproxy.LocatorListener;
import com.codemacro.kvproxy.ServerLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created on 2017/4/15.
 * A locator only for group service
 */
public class GroupLocator implements ServerLocator {
  private static final Logger logger = LoggerFactory.getLogger(GroupLocator.class.getName());
  private Map<String, ServerLocator> locators = new HashMap<String, ServerLocator>();

  /*
    locator.type.#N = locatorType
    locator.name.#N = clientName
    locator.conf.#N = locatorConf
   */
  public void initialize(String conf) {
    try {
      FileInputStream stream = new FileInputStream(new File(conf));
      Properties prop = new Properties();
      prop.load(stream);
      for (int i = 0; ; ++i) {
        String is = String.valueOf(i);
        String type = prop.getProperty("locator.type." + is);
        if (type == null) break;
        String name = prop.getProperty("locator.name." + is);
        String conf0 = prop.getProperty("locator.conf." + is);
        if (name == null || conf0 == null) {
          throw new RuntimeException("require `name` and `conf` for locator: #" + is);
        }
        ServerLocator locator = createLocator(type, conf0);
        logger.info("add locator {} with type {}", name, type);
        locators.put(name, locator);
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void setListener(LocatorListener listener) {
  }

  public ServerLocator getLocator(String name) {
    return locators.get(name);
  }

  public List<String> getList() {
    return null;
  }

  private ServerLocator createLocator(String impl, String conf) {
    try {
      Class clazz = Class.forName(impl);
      ServerLocator locator = (ServerLocator) clazz.newInstance();
      locator.initialize(conf);
      return locator;
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    }
  }
}
