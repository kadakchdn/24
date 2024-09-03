package com.orasi;

import com.google.gson.Gson;
import com.orasi.event.spi.StepEvent;
import com.orasi.model.StepException;
import com.orasi.model.StepPayload;
import com.orasi.selenium4helper.Selenium4Helper;
import static com.orasi.shared_library.getStepCounter;
import static com.orasi.shared_library.notifyListeners;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.MutableCapabilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.safari.SafariOptions;

/**
 * Controls the definition and connection logic for a specific device
 */
public class EndpointDevice {

  private static final Gson gson = new Gson();
  private final Pattern contextPattern = Pattern.compile("#\\{([^}]*)}");
  protected static Logger log = LoggerFactory.getLogger(EndpointDevice.class);
  private static final String URL = "URL";
  private static final String[] KNOWN_PROPERTIES = new String[]{"URL", "username", "accessKey"};

  private final Router router;
  private final ExecutionTarget executionTarget;
  private static final Map<String, Class<? extends MutableCapabilities>> targetMap = new HashMap<>(10);

  static {
    targetMap.put("edge", EdgeOptions.class);
    targetMap.put("msedge", EdgeOptions.class);
    targetMap.put("microsoftedge", EdgeOptions.class);
    targetMap.put("microsoft", EdgeOptions.class);
    targetMap.put("chrome", ChromeOptions.class);
    targetMap.put("chromium", ChromeOptions.class);
    targetMap.put("google", ChromeOptions.class);
    targetMap.put("firefox", FirefoxOptions.class);
    targetMap.put("mozilla", FirefoxOptions.class);
    targetMap.put("safari", SafariOptions.class);
    targetMap.put("mac", SafariOptions.class);
    targetMap.put("macos", SafariOptions.class);
    targetMap.put("apple", SafariOptions.class);
    targetMap.put("opera", DesiredCapabilities.class);
    targetMap.put("*", DesiredCapabilities.class);
  }

  private int queueIndex;

  /**
   * Creates an endpoint definition containing both the router and device
   * connection information
   *
   * @param router
   * @param executionTarget
   */
  public EndpointDevice(Router router, ExecutionTarget executionTarget) {
    this.router = router;
    this.executionTarget = executionTarget;
  }

  /**
   * Defines the maximum amount of connections that this browser definition can
   * handle. The allows tests to be split across multiple devices for faster
   * executions
   *
   * @return
   */
  public int getMaximumAvailable() {
    return getExecutionTarget().getMaximumAvailable();
  }

  /**
   * A friendly name for this device
   *
   * @return
   */
  public String getName() {
    return getExecutionTarget().getName() + " at " + getRouter().getName();
  }

  public String getId() {
    return getExecutionTarget().getAlchemyIdentifier();
  }

  private boolean isKnown(String p) {
    for (String s : KNOWN_PROPERTIES) {
      if (s.equals(p)) {
        return true;
      }
    }

    return false;
  }

  /**
   *
   * @param keyName
   * @return
   */
  public String getProperty(String keyName) {
    String value = getValue(getRouter().getProperty(keyName));
    if (value == null) {
      return getValue(getExecutionTarget().getProperty(keyName), getExecutionTarget().getProperty(keyName));
    } else {
      return value;
    }
  }

  private String getValue(String value) {
    return getValue(value, value);
  }

  private String getValue(String value, String defaultValue) {
    if (value == null) {
      return defaultValue;
    }
    Matcher m = contextPattern.matcher(value);

    if (m.find()) {
      return System.getProperty(m.group(1), defaultValue);
    }

    return defaultValue;
  }

  public WebDriver connect(TestWrapper tW, int textExecutionId) throws StepException {

    StepPayload sP = new StepPayload();
    sP.setActionName("Establishing Connection");
    sP.setExecutionId(SuiteExecutionWrapper.instance().getExecutionId());
    int parentStep = getStepCounter();
    sP.setStepId(parentStep);
    sP.setTestExecutionId(textExecutionId);
    sP.setParentStep(0);
    sP.setStepDetail("{'actionDisplay': 'Establishing Connection to " + getExecutionTarget().getName() + " on " + getRouter().getName() + "' }");
    notifyListeners(new StepEvent(sP, tW.getName(), 1));
    try {

      Class<? extends MutableCapabilities> cC = targetMap.get(getValue(executionTarget.getPropertyMap().get("browserName")).toLowerCase());
      if (cC == null) {
        throw new IllegalArgumentException("Could not find an execution target by name [" + getValue(executionTarget.getPropertyMap().get("browserName")) + "]");
      }

      MutableCapabilities dC;
      try {
        dC = cC.getConstructor((Class[]) null).newInstance((Object[]) null);
      } catch (IllegalAccessException | IllegalArgumentException | InstantiationException | NoSuchMethodException | SecurityException | InvocationTargetException e) {
        throw new IllegalArgumentException("Failed to create instance of " + cC.getName());
      }

      //
      // Add the router properties first
      //
      Map<String, String> varMap = new HashMap<>(10);
      varMap.putAll(getRouter().getPropertyMap());
      varMap.putAll(getExecutionTarget().getPropertyMap());
      String varDetail = gson.toJson(varMap);

      StepPayload stepPayload = new StepPayload();
      stepPayload.setActionName("Initializing Router");
      stepPayload.setExecutionId(SuiteExecutionWrapper.instance().getExecutionId());
      stepPayload.setStepId(getStepCounter());
      stepPayload.setTestExecutionId(textExecutionId);
      stepPayload.setParentStep(parentStep);

      stepPayload.setStepDetail("{'actionDisplay': 'Configuring Router'}");
      notifyListeners(new StepEvent(stepPayload, tW.getName(), 1));

      switch (getRouter().getProviderId()) {

        case 2:
          
          Map<String, Object> sauceOptions = new HashMap<>();
          sauceOptions.put("username", getRouter().getPropertyMap().get("username"));
          sauceOptions.put("accessKey", getRouter().getPropertyMap().get("accessKey"));
          sauceOptions.put("name", tW.getName());
          sauceOptions.put("build", "selenium-build-AH1QO");
          dC.setCapability("sauce:options", sauceOptions);
          break;
      }

      getRouter().getPropertyMap().keySet().stream().filter(key -> (!isKnown(key))).map(key -> {
        log.info("Adding ROUTER Capability [" + key + "] as [" + getValue(getRouter().getPropertyMap().get(key)) + "]");
        return key;
      }).forEachOrdered(key -> {
        dC.setCapability(key, getValue(getRouter().getPropertyMap().get(key)));
      });

      stepPayload.setStatus(1);
      notifyListeners(new StepEvent(stepPayload, tW.getName(), 4));

      //
      // We are going to check the available browsers here to make sure we have a match
      //
      switch (getRouter().getProviderId()) {
        case 2:
          break;
        default:
          try {
            stepPayload = new StepPayload();
            stepPayload.setActionName("Analyzing Preflight");
            stepPayload.setExecutionId(SuiteExecutionWrapper.instance().getExecutionId());
            stepPayload.setStepId(getStepCounter());
            stepPayload.setTestExecutionId(textExecutionId);
            stepPayload.setParentStep(parentStep);

            stepPayload.setVariableList(varDetail);
            stepPayload.setStepDetail("{'actionDisplay': 'Querying pre-flight details for {var:browserName} from {var:URL}'}");
            notifyListeners(new StepEvent(stepPayload, tW.getName(), 1));

            if (!Selenium4Helper.isBrowserSupported(getValue(router.getPropertyMap().get(URL)), getValue(executionTarget.getPropertyMap().get("browserName")))) {
              throw new IllegalArgumentException("Expected [" + getValue(getExecutionTarget().getPropertyMap().get("browserName")) + "] but only [" + String.join(",", Selenium4Helper.getSupportedBrowsers(getValue(getRouter().getPropertyMap().get(URL)))) + "] were available");
            }

            stepPayload.setStatus(1);
            notifyListeners(new StepEvent(stepPayload, tW.getName(), 4));
          } catch (IOException e) {
            stepPayload.setStatus(0);
            stepPayload.setMessage(e.getMessage());
            notifyListeners(new StepEvent(stepPayload, tW.getName(), 4));
            log.atWarn().log("Could not aquire pre-flight details from router: " + e.getMessage());
            log.atDebug().log("Could not aquire pre-flight details from router ", e.getMessage());
          }
      }

      getExecutionTarget().getPropertyMap().keySet().stream().filter(key -> (!isKnown(key))).map(key -> {
        log.info("Adding TARGET Capability [" + key + "] as [" + getValue(getExecutionTarget().getPropertyMap().get(key)) + "]");
        return key;
      }).forEachOrdered(key -> {
        dC.setCapability(key, getValue(getExecutionTarget().getPropertyMap().get(key)));
      });

      try {
        stepPayload = new StepPayload();
        stepPayload.setActionName("Connect");
        stepPayload.setExecutionId(SuiteExecutionWrapper.instance().getExecutionId());
        stepPayload.setStepId(getStepCounter());
        stepPayload.setTestExecutionId(textExecutionId);
        stepPayload.setParentStep(parentStep);

        stepPayload.setVariableList(varDetail);
        stepPayload.setStepDetail("{'actionDisplay': 'Connect to {var:browserName} on {var:URL}'}");
        notifyListeners(new StepEvent(stepPayload, tW.getName(), 1));
        WebDriver webDriver = new RemoteWebDriver(new URL(getProperty(URL)), dC);
        webDriver.manage().timeouts().implicitlyWait(Duration.ofMillis(1000));
        stepPayload.setStatus(1);
        notifyListeners(new StepEvent(stepPayload, tW.getName(), 4));
        notifyListeners(new StepEvent(sP, tW.getName(), 4));
        return webDriver;
      } catch (Exception e) {

        StringBuilder errorBuilder = new StringBuilder();
        errorBuilder.append("Error connecting to ").append(getExecutionTarget().getName()).append(" at ").append(getRouter().getName()).append(" using:\r\n");

        getRouter().getPropertyMap().keySet().forEach(key -> {
          errorBuilder.append("\tROUTER: ").append(key).append(" = [").append(getValue(getRouter().getPropertyMap().get(key))).append("]\r\n");
        });

        getExecutionTarget().getPropertyMap().keySet().forEach(key -> {
          errorBuilder.append("\tTARGET: ").append(key).append(" = [").append(getValue(getExecutionTarget().getPropertyMap().get(key))).append("]\r\n");
        });
        errorBuilder.append(e.getMessage()).append("\r\n");
        
        log.atError().log( errorBuilder.toString(), e );

        stepPayload.setStatus(-1);
        stepPayload.setMessage(errorBuilder.toString());
        notifyListeners(new StepEvent(stepPayload, tW.getName(), 3));
        throw new StepException(stepPayload.getStepId(), StepException.Threshold.FATAL, e, errorBuilder.toString(), stepPayload, StepException.FailureType.Infrastructure);
      }
    } catch (StepException e) {
      notifyListeners(new StepEvent(sP, tW.getName(), 3));
      throw e;
    }

  }

  /**
   * Disconnect and close the current connection
   *
   * @param webDriver
   */
  public void disconnect(WebDriver webDriver) {
    try {
      log.info("Closing connection to " + getExecutionTarget().getName() + " at " + getRouter().getName());
      webDriver.close();
    } catch (Exception e) {
    }
    try {
      log.info("Destroying WebDriver instance for " + getExecutionTarget().getName() + " at " + getRouter().getName());
      webDriver.quit();
    } catch (Exception e) {
    }
  }

  /**
   * @return the routerDetail
   */
  public String getRouterDetail() {
    return getRouter().getRouterDetail();
  }

  /**
   * @return the targetDetail
   */
  public String getTargetDetail() {
    return getExecutionTarget().getTargetDetail();
  }

  /**
   * @return the router
   */
  public Router getRouter() {
    return router;
  }

  /**
   * @return the executionTarget
   */
  public ExecutionTarget getExecutionTarget() {
    return executionTarget;
  }

}
