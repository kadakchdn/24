package com.orasi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Router {

  private final Map<String, String> propertyMap = new HashMap<>(10);
  private final String name;
  private final String alchemyIdentifier;
  private final String routerDetail;
  private final int providerId;
  private List<ExecutionTarget> targetList = new ArrayList<>(5);

  public Router(int providerId, String largeName, String alchemyIdentifier, String routerDetail) {
    this.providerId = providerId;
    this.name = largeName;
    this.alchemyIdentifier = alchemyIdentifier;
    this.routerDetail = routerDetail;
  }

  public boolean addTarget(ExecutionTarget eT) {
    if (eT.getRouterIdentifier().equals(alchemyIdentifier)) {
      targetList.add(eT);
      eT.setRouter(this);
      return true;
    } else {
      return false;
    }
  }

  public void addProperty(String name, String value) {

    for (String keyName : propertyMap.keySet()) {
      String pValue = propertyMap.get(keyName);
      if (pValue != null) {
        value = value.replace("${" + keyName + "}", pValue);
      }
    }

    propertyMap.put(name, value);
  }

  public String getProperty(String name) {
    return propertyMap.get(name);
  }

  /**
   * @return the propertyMap
   */
  public Map<String, String> getPropertyMap() {
    return Collections.unmodifiableMap(propertyMap);
  }

  /**
   * @return the maximumAvailable
   */
  public String getAlchemyIdentifier() {
    return alchemyIdentifier;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  public String getRouterDetail() {
    return routerDetail;
  }

  /**
   * @return the targetList
   */
  public List<ExecutionTarget> getTargetList() {
    return targetList;
  }

  /**
   * @param targetList the targetList to set
   */
  public void setTargetList(List<ExecutionTarget> targetList) {
    this.targetList = targetList;
  }

  /**
   * @return the providerId
   */
  public int getProviderId() {
    return providerId;
  }

}
