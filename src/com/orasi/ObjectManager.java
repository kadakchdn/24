
package com.orasi;

import java.util.*;
import org.openqa.selenium.By;
import org.openqa.selenium.By.*;
import org.openqa.selenium.*;
import com.orasi.datasource.*;


public class ObjectManager {
  private static final ObjectManager singleton = new ObjectManager();
  
  public static final ObjectManager instance() {
    return singleton;
  }
  
  private final Map<String,ByFactory> objectMap = new HashMap<>( 10 );
  
  private ObjectManager() {
    ByFactoryCollection bC = null;
    /*
    Site: www.google.com
    Add a description of www.google.com
    */
    /* Page: Google 
    
    */

    

bC = new ByFactoryCollection("q", "16292.815");
bC.add( new ByFactory( ByXPath.class, "//TEXTAREA[@id=\"APjFqb\"]", "IDRule", "16292.817" ) );
bC.add( new ByFactory( ById.class, "APjFqb", "IDRule", "16292.819" ) );
bC.add( new ByFactory( ByXPath.class, "//*[@id=\"APjFqb\"]", "IDRule", "16292.821" ) );
bC.add( new ByFactory( ByXPath.class, "//TEXTAREA[@name=\"q\"]", "NameRule", "16292.823" ) );
bC.add( new ByFactory( ByName.class, "q", "NameRule", "16292.825" ) );
bC.add( new ByFactory( ByXPath.class, "//*[@name=\"q\"]", "NameRule", "16292.827" ) );


objectMap.put( "16292.815", bC );




  }
  
  public By getObject( Object alchemyIdentifier, Map<String,Object> contextMap, DataSourceProvider dM ) {
    
    if ( alchemyIdentifier instanceof By ) {
      return (By) alchemyIdentifier;
    }
    
    ByFactory by = objectMap.get( alchemyIdentifier + "" );
    if ( by == null ) {
      return new By() {
        @Override
        public List<WebElement> findElements(SearchContext sc) {
          throw new RuntimeException( "Could not find and object using [" + alchemyIdentifier + "]" );
        }
      };
    }
    return by.create(contextMap, dM);
  }

  public ByFactory getObject( String alchemyIdentifier ) {
   
    return objectMap.get( alchemyIdentifier );
  }
}
