package com.orasi;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebElement;

/**
 * The By collections provides an order list of locators used to find a specific
 * element
 *
 */
public class ByCollection extends By implements Serializable, ByIdentifier, ByTimer {

  private static final long serialVersionUID = 4573668832699497306L;

  private final By[] bys;
  private final String name;
  private final String alchemyId;
  private long startTime;
  private long searchTime;

  /**
   * A collection of locators used to find a specific element
   *
   * @param name The friendly name of this identifier
   * @param alchemyId The internal unique id for this identifier
   * @param bys
   */
  public ByCollection(String name, String alchemyId, By... bys) {
    this.name = name;
    this.alchemyId = alchemyId;
    this.bys = bys;
  }

  /**
   * @see org.openqa.selenium.By
   * @param context
   * @return
   */
  @Override
  public WebElement findElement(SearchContext context) {
    List<WebElement> elements = findElements(context);
    if (elements.isEmpty()) {
      throw new NoSuchElementException("Cannot locate an element using " + toString());
    }
    return elements.get(0);
  }

  /**
   * @see org.openqa.selenium.By
   * @param context
   * @return
   */
  @Override
  public List<WebElement> findElements(SearchContext context) {
    List<WebElement> elems = new ArrayList<>();
    for (By by : bys) {
      try {
        List<WebElement> elementList = by.findElements(context);
        if (elementList != null && !elementList.isEmpty()) {
          elems.addAll(by.findElements(context));
          return elems;
        }
      } catch (Exception e) {

      }
    }

    return elems;
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder("By.collection(");
    stringBuilder.append("{");

    boolean first = true;
    for (By by : bys) {
      stringBuilder.append((first ? "" : ",")).append(by);
      first = false;
    }
    stringBuilder.append("})");
    return stringBuilder.toString();
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
}
