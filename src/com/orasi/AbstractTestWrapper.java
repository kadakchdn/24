package com.orasi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.orasi.datasource.DataRow;
import com.orasi.datasource.DataSourceProvider;
import com.orasi.datasource.DataTable;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import org.openqa.selenium.By;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * AbstractTestWrapper provides some base functionality for each test or
 * function
 *
 */
public abstract class AbstractTestWrapper implements TestWrapper {

  protected static Logger log = LoggerFactory.getLogger(TestWrapper.class);
  protected GsonBuilder gsonBuilder = new GsonBuilder();
  protected Gson gson = gsonBuilder.create();
  private final int id;
  private final String name;
  private final String description;
  private final String dataName;
  private final int synchronizationPoint;
  private final String[] tags;
  private final String alchemyIdentifier;
  private final String testDetail;

  /**
   * The entry point for a test/function execution
   *
   * @param executionId The suite execution identifier
   * @param testExecutionId The test execution identifier
   * @param webDriver The selenium web driver instance
   * @param contextMap A name/value pair of test scoped context variables
   * @param contextName The current context name used for function calls and
   * recursive executions
   * @param callStack A call stack of test/function names
   * @param stepStack A call stack of step execution identifiers
   */
  protected abstract void _executeTest(int executionId, int testExecutionId, WebDriver webDriver, Map<String, Object> contextMap, String contextName, Stack<String> callStack, Stack<Integer> stepStack);

  /**
   *
   * @param id An identifier for this test
   * @param alchemyIdentifier The alchemy identifier for this test
   * @param name The name of this test
   * @param description A description of this test
   * @param synchronizationPoint The synchronization point for this test
   * @param dataName If this test is to be repeated, this is the alchemy
   * identifier for the data source
   * @param tags An array of tags for this test
   * @param testDetail A JSON string of test details
   */
  public AbstractTestWrapper(int id, String alchemyIdentifier, String name, String description, int synchronizationPoint, String dataName, String[] tags, String testDetail) {
    this.id = id;
    this.name = name;
    this.dataName = dataName;
    this.description = description;
    this.synchronizationPoint = synchronizationPoint;
    this.tags = tags;
    this.alchemyIdentifier = alchemyIdentifier;
    this.testDetail = testDetail;
  }
  
  
  protected Map<String,Object> createMap( String... nvPairs ) {
    Map<String,Object> variableMap = new HashMap<>( nvPairs.length );
    for ( String s : nvPairs ) {
      String[] s2 = s.split( "=" );
      if ( s2.length > 1 ) {
        variableMap.put( s2[0], s2[1] );
      }
    }
    
    return variableMap;
  }
  
  /**
   * Returns a Number given a string value and classType
   *
   * @param <T>
   * @param value
   * @param numberType
   * @return
   */
  protected <T extends Number> T parseNumber(Object value, Class<T> numberType) {
    if (value == null) {
      return null;
    }

    if (numberType.isAssignableFrom(value.getClass())) {
      return (T) value;
    }

    if (value instanceof String) {

      if (value.equals("")) {
        return null;
      }

      switch (numberType.getSimpleName()) {
        case "Long":

          return (T) (Long) Double.valueOf((String) value).longValue();
        case "Short":
          return (T) (Short) Double.valueOf((String) value).shortValue();
        case "Byte":
          return (T) Byte.valueOf((String) value);
        case "Double":
          return (T) Double.valueOf((String) value);
        case "Float":
          return (T) (Float) Double.valueOf((String) value).floatValue();
      }
    } else {
      switch (numberType.getSimpleName()) {
        case "Long":
          return (T) (Long) Double.valueOf(value + "").longValue();
        case "Short":
          return (T) (Short) Double.valueOf(value + "").shortValue();
        case "Byte":
          return (T) Byte.valueOf(value + "");
        case "Double":
          return (T) Double.valueOf(value + "");
        case "Float":
          return (T) (Float) Double.valueOf(value + "").floatValue();
      }
    }

    throw new IllegalArgumentException("Failed to parse number as " + numberType.getName() + " using " + value);
  }

  /**
   * The entry point for a test/function execution
   *
   * @param executionId The suite execution identifier
   * @param testExecutionId The test execution identifier
   * @param webDriver The selenium web driver instance
   * @param contextMap A name/value pair of test scoped context variables
   * @param contextName The current context name used for function calls and
   * recursive executions
   * @param callStack A call stack of test/function names
   * @param stepStack A call stack of step execution identifiers
   */
  @Override
  public void executeTest(int executionId, int testExecutionId, WebDriver webDriver, Map<String, Object> contextMap, String contextName, Stack<String> callStack, Stack<Integer> stepStack) {
    this._executeTest(executionId, testExecutionId, webDriver, contextMap, contextName, callStack, stepStack);
  }

  /**
   * The entry point for a test/function execution
   *
   * @param executionId The suite execution identifier
   * @param testExecutionId The test execution identifier
   * @param webDriver The selenium web driver instance
   */
  @Override
  public void executeTest(int executionId, int testExecutionId, WebDriver webDriver) {
    log.info("Starting Test " + getName());
    _executeTest(executionId, testExecutionId, webDriver, new ContextMap(), "", new Stack<>(), new Stack<>());
  }

  /**
   *
   * @return
   */
  @Override
  public String[] getTags() {
    return tags;
  }

  /**
   *
   * @return
   */
  @Override
  public String getTestDetail() {
    return testDetail;
  }

  /**
   *
   * @return
   */
  @Override
  public String getDescription() {
    return description;
  }

  /**
   *
   * @return
   */
  @Override
  public int getId() {
    return id;
  }

  /**
   *
   * @return
   */
  @Override
  public String getDataName() {
    return dataName;
  }

  /**
   *
   * @return
   */
  @Override
  public String getAlchemyIdentifier() {
    return alchemyIdentifier;
  }

  /**
   *
   * @return
   */
  @Override
  public int getSynchronizationPoint() {
    return synchronizationPoint;
  }

  /**
   *
   * @return
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   *
   * @return
   */
  @Override
  public DataRow getData() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  /**
   *
   * @return
   */
  @Override
  public int getDataIndex() {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  protected VariableWrapper createVariable(String name, int type, boolean required, String value, DataSourceProvider dS, Map<String, Object> contextMap, WebDriver webDriver) {
    log.atDebug().log("Creating variable [" + name + "] from [" + value + "] of type " + type);

    Object useValue = null;
    String useName = dS.replaceValues(value, contextMap) + "";

    switch (type) {
      case 2:
        if (value == null) {
          if (required) {
            throw new IllegalArgumentException("No value was provided for [" + name + "] - expected a whole number");
          } else {
            return null;
          }
        }

        if (value.getClass().isAssignableFrom(Long.class)) {
          useValue = value;
        } else {
          try {
            useValue = (Long) Double.valueOf(dS.replaceValues(value, contextMap) + "").longValue();
          } catch (NumberFormatException e) {
            if (required) {
              throw new IllegalArgumentException("[" + value + "] was provided for [" + name + "] - expected a whole number", e);
            }
          }
        }
        break;

      case 3:
        if (value == null) {
          if (required) {
            throw new IllegalArgumentException("No value was provided for [" + name + "] - expected a decimal");
          } else {
            return null;
          }
        }

        if (value.getClass().isAssignableFrom(Double.class)) {
          useValue = value;
        } else {

          try {
            useValue = Double.valueOf(dS.replaceValues(value, contextMap) + "");
          } catch (NumberFormatException e) {
            if (required) {
              throw new IllegalArgumentException("[" + value + "] was provided for [" + name + "] - expected a decimal", e);
            }
          }
        }
        break;

      case 4:
        if (value == null) {
          log.atWarn().log("No value was provided for [" + name + "] - assuming false");
          useValue = false;
        } else {
          if (value.getClass().isAssignableFrom(Boolean.class)) {
            useValue = value;
          } else {
            useValue = Boolean.valueOf(dS.replaceValues(value, contextMap) + "");
          }
        }

        break;
      case 5:
        if (value == null) {
          if (required) {
            throw new IllegalArgumentException("No value was provided for [" + name + "] - expected a Locator reference");
          } else {
            return null;
          }
        }
        if (value.getClass().isAssignableFrom(By.class)) {
          useValue = value;
        } else {
          useValue = ObjectManager.instance().getObject(dS.replaceValues(value, contextMap), contextMap, dS);

          if (useValue == null) {
            if (required) {
              throw new RuntimeException("[" + value + "] was provided for [" + name + "], but no Object was found using that locator reference");
            }
          }

          ByFactory bF = ObjectManager.instance().getObject(dS.replaceValues(value, contextMap) + "");
          if (bF != null) {
            useName = bF.getName();
          }
        }
        break;
      case 6:
        if (value == null) {
          if (required) {
            throw new IllegalArgumentException("No value was provided for [" + name + "] - expected a Function reference");
          }
        }
        useValue = dS.replaceValues(value, contextMap) + "";
        break;

      case 8:
        if (value == null) {
          if (required) {
            throw new IllegalArgumentException("No value was provided for [" + name + "] - expected a Locator reference");
          } else {
            return null;
          }
        }
        if (value.getClass().isAssignableFrom(WebElement.class)) {
          useValue = value;
        } else {
          useValue = ObjectManager.instance().getObject(dS.replaceValues(value, contextMap), contextMap, dS);

          if (useValue == null) {
            if (required) {
              throw new IllegalArgumentException("[" + value + "] was provided for [" + name + "], but no Object was found using that locator reference");
            }
          } else {
            ByFactory bF = ObjectManager.instance().getObject(dS.replaceValues(value, contextMap) + "");
            if (bF != null) {
              useName = bF.getName();
            }

            try {
              webDriver.findElement((By) useValue);
            } catch (Exception e) {
              throw new IllegalArgumentException("[" + value + "] was provided for [" + name + "].  The element on the page could not be found using [" + useValue.toString() + "]");
            }
          }
        }
        break;

      case 9:
        if (value == null) {
          if (required) {
            throw new IllegalArgumentException("No value was provided for [" + name + "] - expected a Data Table reference");
          } else {
            return null;
          }
        }
        if (value.getClass().isAssignableFrom(DataTable.class)) {
          useValue = value;
        } else {
          useValue = dS.getTable(dS.replaceValues(value, contextMap) + "");

          if (useValue == null) {
            if (required) {
              throw new RuntimeException("[" + value + "] was provided for [" + name + "], but no Data Table was found using that table referenace");
            }
          } else {
            useName = ((DataTable) useValue).getId();
          }
        }
        break;

      default:
        if (value != null) {
          useValue = dS.replaceValues(value, contextMap) + "";
        }
        break;
    }

    return new VariableWrapper(name, useValue, useName);
  }
}
