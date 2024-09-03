package com.orasi;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import org.openqa.selenium.By;
import com.orasi.datasource.*;

/**
 * A class used to construct a locator compatible with Selenium that has been scanned and processed replacing and alchemy specific replaceable values such as data, context, etc
 */
public class ByFactory {

  private final Class byClass;
  private final String descriptor;
  private final String name;
  private final String alchemyId;

  /**
   * 
   * @param byClass A class compatible with By
   * @param descriptor The textual descriptor definition
   * @param name A friendly name for this locator
   * @param alchemyId A unique identifier for this locator
   */
  public ByFactory(Class byClass, String descriptor, String name, String alchemyId) {
    this.byClass = byClass;
    this.descriptor = descriptor;
    this.name = name;
    this.alchemyId = alchemyId;
  }

  /**
   * Given the current context map and a data source provider, this generates a locator
   * @param contextMap
   * @param dM
   * @return
   */
  public By create(Map<String, Object> contextMap, DataSourceProvider dM) {
    String useDescriptor = dM.replaceValues(descriptor, contextMap) + "";
    try {
      return (By) byClass.getConstructor(String.class).newInstance(useDescriptor);
    } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
      throw new IllegalArgumentException( "Could not create locator for " + useDescriptor + " as " + byClass.getName() );
    }
  }

  /**
   *
   * @return
   */
  public String getDescriptor() {
    return descriptor;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the alchemyId
   */
  public String getAlchemyId() {
    return alchemyId;
  }
}
