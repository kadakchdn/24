package com.orasi;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.HasCapabilities;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Interactive;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.remote.RemoteWebDriver;

public class MonitoredRemoteWebDriver implements WebDriver, JavascriptExecutor, HasCapabilities, Interactive, TakesScreenshot {

  private final RemoteWebDriver webDriver;
  private final ExecutionWrapper eW;

  private static class CachedElement {

    private final String identifier;
    private final WebElement webElement;

    private CachedElement(String identifier, WebElement webElement) {
      this.identifier = identifier;
      this.webElement = webElement;
    }

    /**
     * @return the identifier
     */
    public String getIdentifier() {
      return identifier;
    }

    /**
     * @return the webElement
     */
    public WebElement getWebElement() {
      return webElement;
    }
  }

  private final ThreadLocal<CachedElement> cachedElement = new ThreadLocal();

  public MonitoredRemoteWebDriver(RemoteWebDriver webDriver, ExecutionWrapper eW) {
    this.webDriver = webDriver;
    this.eW = eW;
  }

  @Override
  public void get(String string) {
    webDriver.get(string);
  }

  @Override
  public String getCurrentUrl() {
    return webDriver.getCurrentUrl();
  }

  @Override
  public String getTitle() {
    return webDriver.getTitle();
  }

  @Override
  public List<WebElement> findElements(By by) {
    return webDriver.findElements(by);
  }

  @Override
  public WebElement findElement(By by) {

    if (by instanceof ByIdentifier) {
      String id = ((ByIdentifier) by).getAlchemyId();

      if (cachedElement.get() != null) {
        if (cachedElement.get().getIdentifier().equals(id)) {
          return cachedElement.get().getWebElement();
        } else {
          cachedElement.set(null);
        }
      }

      WebElement wE = webDriver.findElement(by);

      if (wE != null) {
        cachedElement.set(new CachedElement(id, wE));
      }

      return wE;
    }

    return webDriver.findElement(by);
  }

  @Override
  public String getPageSource() {
    return webDriver.getPageSource();
  }

  @Override
  public void close() {
    webDriver.close();
  }

  @Override
  public void quit() {
    webDriver.quit();
  }

  @Override
  public Set<String> getWindowHandles() {
    return webDriver.getWindowHandles();
  }

  @Override
  public String getWindowHandle() {
    return webDriver.getWindowHandle();
  }

  @Override
  public TargetLocator switchTo() {
    return webDriver.switchTo();
  }

  @Override
  public Navigation navigate() {
    return webDriver.navigate();
  }

  @Override
  public Options manage() {
    return webDriver.manage();
  }

  @Override
  public Object executeScript(String string, Object... os) {
    return webDriver.executeScript(string, os);
  }

  @Override
  public Object executeAsyncScript(String string, Object... os) {
    return webDriver.executeAsyncScript(string, os);
  }

  @Override
  public Capabilities getCapabilities() {
    return webDriver.getCapabilities();
  }

  @Override
  public void perform(Collection<Sequence> clctn) {
    webDriver.perform(clctn);
  }

  @Override
  public void resetInputState() {
    webDriver.resetInputState();
  }

  @Override
  public <X> X getScreenshotAs(OutputType<X> ot) throws WebDriverException {
    return webDriver.getScreenshotAs(ot);
  }
}
