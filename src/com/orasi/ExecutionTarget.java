package com.orasi;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class ExecutionTarget {
  private final Map<String,String> propertyMap = new HashMap<>( 10 );
  private final int maximumAvailable;
  private String name;
  private final String alchemyIdentifier;
  private final String routerIdentifier;
  private String targetDetail;
  private transient Router router;
  
  public ExecutionTarget( String name, String alchemyIdentifier, String routerIdentifier, int maximumAvailable, String targetDetail ) {
    this.name = name;
    this.maximumAvailable = maximumAvailable;
    this.alchemyIdentifier = alchemyIdentifier;
    this.routerIdentifier = routerIdentifier;
    this.targetDetail = targetDetail;
  }
  
  public void addProperty( String name, String value ) { 
    
    for ( String keyName : propertyMap.keySet() ) {
      String pValue = propertyMap.get( keyName );
      if ( pValue != null ) {
        value = value.replace( "${" + keyName + "}", pValue );
      }
    }
    
    if ( router != null ) {
      for ( String keyName : router.getPropertyMap().keySet() ) {
        String pValue = router.getPropertyMap().get( keyName );
        if ( pValue != null ) {
          value = value.replace( "${" + keyName + "}", pValue );
        }
      }
    }
    
    
    propertyMap.put( name, value );
  }

  public String getTargetDetail() {
    return targetDetail;
  }
  
  public String getProperty( String name ) {
    return propertyMap.get( name );
  }

  /**
   * @return the propertyMap
   */
  public Map<String,String> getPropertyMap() {
    return Collections.unmodifiableMap(propertyMap);
  }

  /**
   * @return the maximumAvailable
   */
  public int getMaximumAvailable() {
    return maximumAvailable;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  public String getAlchemyIdentifier() {
    return alchemyIdentifier;
  }

  /**
   * @return the routerIdentifier
   */
  public String getRouterIdentifier() {
    return routerIdentifier;
  }

  /**
   * @return the router
   */
  public Router getRouter() {
    return router;
  }

  /**
   * @param router the router to set
   */
  public void setRouter(Router router) {
    this.router = router;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    targetDetail = targetDetail.replace( "\"name\":\"" + this.name + "\"", "\"name\":\"" + name + "\"" );
    this.name = name;
  }
}
