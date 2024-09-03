
package com.orasi;

import java.io.Serializable;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

/**
 *
 * @author allen
 */
public class Py extends By implements Serializable, ByTimer, ByIdentifier {

  private final By by;
  private long startTime;
  private long searchTime;
  private final String name;
  private final String alchemyId;
          
  public Py( String name, String alchemyId, By by ) {
    this.by = by;
    this.name = name;
    this.alchemyId = alchemyId;
  }
  
  @Override
  public WebElement findElement(SearchContext context) {
    startTime = System.currentTimeMillis();
    WebElement wE = getBy().findElement(context);
    searchTime = System.currentTimeMillis() - getStartTime();
    return wE;
  }

  @Override
  public List<WebElement> findElements(SearchContext context) {
    startTime = System.currentTimeMillis();
    List<WebElement> wE = getBy().findElements(context);
    searchTime = System.currentTimeMillis() - getStartTime();
    return wE;
  }

  /**
   * @return the startTime
   */
  @Override
  public long getStartTime() {
    return startTime;
  }

  /**
   * @return the searchTime
   */
  @Override
  public long getSearchTime() {
    return searchTime;
  }

  /**
   * @return the by
   */
  public By getBy() {
    return by;
  }

  /**
   * @return the name
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * @return the alchemyId
   */
  @Override
  public String getAlchemyId() {
    return alchemyId;
  }
  
  
}
