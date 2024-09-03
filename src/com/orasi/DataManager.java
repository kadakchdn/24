package com.orasi;

import com.orasi.datasource.DataField;
import com.orasi.datasource.DataRow;
import com.orasi.datasource.DataTable;
import com.orasi.datasource.DataSource;
import com.orasi.datasource.DataSourceProvider;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DataManager provides functionality to translate tokenized data, context variables, environment variables, and synthetic data to their actual 
 * values at execution time.  
 * 
 * When looking up dataEach lookup, regardless of type, will first check for a System environment variable with the same name as the lookup
 * and use that value if found.  This allows users to override values at run time using command line or system environment variables.  If not, it 
 * will next check the test scoped context variable list for a matching value and use that if found.  Finally, it will use the targeted lookup from 
 * the specified data source or environment.
 */
public class DataManager implements DataSourceProvider {

  private static final DataManager singleton = new DataManager();

  /**
   * The singleton instance of the data manager used across all suite test executions
   * @return
   */
  public static final DataManager instance() {
    return singleton;
  }

  private DataManager() {

  }

  private final ThreadLocal<Map<String, DataRow>> dataSet = new ThreadLocal<>();

  private static final Logger log = LoggerFactory.getLogger(DataSource.class);

  private static final Pattern DATA_PATTERN = Pattern.compile("(?<DATA>\\$\\{([^}]*)\\})|(?<ENVIRONMENT>\\%\\{([^}]*)\\})|(?<CONTEXT>\\#\\{([^}]*)\\})|(?<SYNTH>\\*\\{([^}]*)\\})");
  private final Map<String, DataSource> providerMap = new HashMap<>(10);
  private final Map<String, String> defaultEnvironmentMap = new HashMap<>(10);
  private final Map<String, String> environmentMap = new HashMap<>(10);

  /**
   * Allows the suite to register a data source
   * @param name
   * @param h
   */
  public void registerDataSource(String name, DataSource h) {
    this.providerMap.put(name, h);
  }

  /**
   * Adds a default environment variable and value
   * @param name
   * @param value
   */
  public void addDefaultEnvironmentProperty(String name, String value) {
    this.defaultEnvironmentMap.put(name, value);
  }

  /**
   * Allows the user to register override property maps for use in different environment configurations
   * @param envMap
   */
  public void addEnvironmentProperties(Map<String, String> envMap) {
    this.environmentMap.putAll(envMap);
  }

  /**
   * Returns a non singleton instance of the data manager
   * @return
   */
  public DataManager getPrivateInstance() {
    return new DataManager();
  }

  /**
   * Clears the thread local data storage
   */
  public void initializeTest() {
    dataSet.set(new HashMap<>(5));
  }

  /**
   * Used to manually put a row into the local data set during execution
   *
   * @param fieldName
   * @param row
   */
  @Override
  public void setRow(String fieldName, DataRow row) {
    dataSet.get().put(fieldName, row);
  }

  /**
   * Gets the thread local row by name
   * @param fieldName
   * @return
   */
  @Override
  public DataRow getRow(String fieldName) {
    return dataSet.get().get(fieldName);
  }

  /**
   * A cleanup method to execute after a test has completed
   */
  public void afterTest() {
    Map<String, DataRow> dataMap = dataSet.get();

    if (dataMap != null) {
      //
      // We need to release any values first
      //
      dataMap.forEach((k, v) -> {
        if ( v != null ) {
          v.release();
        }
      });
    }
  }

  /**
   * Returns a list of field values represented by the field name formatted as
   * [collection seed].[collection id]-[source seed].[source id]
   *
   * @param fieldName
   * @param contextMap
   * @return
   */
  public List<Map<String, Object>> getSource(String fieldName, Map<String, Object> contextMap) {

    if (fieldName == null || fieldName.trim().isEmpty()) {
      return null;
    }

    if (log.isDebugEnabled()) {
      log.debug("Attempting to find a values for [" + fieldName + "]");
    }

    List<Map<String, Object>> lM = (List<Map<String, Object>>) getContext(fieldName, contextMap);

    if (lM == null) {

      String[] lookupMap = fieldName.split("-");

      if (lookupMap.length != 2) {
        throw new IllegalArgumentException("A data source lookup must be formatted as [source seed].[source id]-[table seed].[table id]");
      }

      DataSource<String, DataTable> dS = providerMap.get(lookupMap[0]);
      if (dS == null) {
        throw new IllegalArgumentException("No data source was registered for [" + lookupMap[0] + "]");
      }

      DataTable h = dS.getTable(lookupMap[1]);

      if (h == null) {
        throw new IllegalArgumentException("No table was found for [" + lookupMap[1] + "] in [" + lookupMap[0] + "]");
      }

      return h.getRawData();
    } else {
      return lM;
    }

  }

  /**
   * Execute the data replacement algorithms, ignoring test context maps
   * @param initialValue
   * @return
   */
  public Object replaceValues(String initialValue) {
    return replaceValues(initialValue, null);
  }

  /**
   * Execution the data replacement algorithms
   * @param key The value to replace
   * @param contextMap A set of test scoped context variables
   * @return
   */
  @Override
  public Object replaceValues(String key, Map<String, Object> contextMap) {

    if (key == null) {
      return null;
    }

    String returnValue = key;

    Matcher m = DATA_PATTERN.matcher(returnValue);
    while (m.find()) {
      if (m.group("CONTEXT") != null) {
        Object newValue = getContext(m.group(6), contextMap);
        if (newValue != null) {
          if (m.group(0).equals(key)) {
            return newValue;
          } else {
            returnValue = returnValue.replace(m.group("CONTEXT"), newValue.toString());
          }
        }
      }

      if (m.group("DATA") != null) {
        Object newValue = getValue(m.group(2), contextMap);
        if (newValue != null && !newValue.equals(m.group(2))) {
          if (m.group(0).equals(key)) {
            return newValue;
          } else {
            returnValue = returnValue.replace(m.group("DATA"), newValue.toString());
          }
        } else {
          throw new IllegalArgumentException( "No DATA value defined for " + m.group(2) );
        }
      }

      if (m.group("ENVIRONMENT") != null) {
        Object newValue = getEnvironment(m.group(4), contextMap);
        if (newValue != null) {
          if (m.group(0).equals(key)) {
            return newValue;
          } else {
            returnValue = returnValue.replace(m.group("ENVIRONMENT"), newValue.toString());
          }
        } else {
          throw new IllegalArgumentException( "No ENVIRONMENT value defined for " + m.group(4) );
        }
      }

      if (m.group("SYNTH") != null) {
        SyntheticDataProvider sP = SyntheticFactory.instance().getProvider(m.group(8));
        Object newValue = null;
        if (sP != null) {
          newValue = sP.getData();
        }
        if (newValue != null) {
          if (m.group(0).equals(key)) {
            return newValue;
          } else {
            returnValue = returnValue.replace(m.group("SYNTH"), newValue.toString());
          }
        } else {
          throw new IllegalArgumentException( "No SYNTHETIC provider defined for " + m.group(8) );
        }
      }
    }

    return returnValue;
  }

  private Object getContext(String key, Map<String, Object> contextMap) {

    Object rV = getSystemProperty(key);

    if (rV != null) {
      return rV;
    }

    if (contextMap == null) {
      return null;
    }

    Object returnValue = contextMap.get(key);
    if (returnValue != null) {
      if (log.isDebugEnabled()) {
        log.debug("Value found as [" + returnValue + "] from the context map");
      }
      return returnValue;
    }

    return null;
  }

  private Object getSystemProperty(String key) {
    Object returnValue = System.getProperty(key);
    if (returnValue != null) {
      if (log.isDebugEnabled()) {
        log.debug("Value found as [" + returnValue + "] from the system/command line properties");
      }
      return returnValue;
    }

    return null;
  }


  /**
   * Returns a value in STRING form. This value is mapped by 3 parts separated
   * by -. ${[collection seed].[collection id]-[source seed].[source id]-[field
   * name]}
   *
   * @param fieldName
   * @param contextMap
   * @return
   */
  private Object getValue(String fieldName, Map<String, Object> contextMap) {
    String useValue = fieldName;

    if (useValue.startsWith("${")) {
      useValue = useValue.substring(2, useValue.length() - 1);
    }
    
    //
    // Before we use secrets, we check if a context variable or system property override this
    //
    Object returnValue = getContext(useValue, contextMap);
    if (returnValue != null) {
      return returnValue;
    }
    

    String[] lookupMap = useValue.split("-");

    if (lookupMap.length != 3) {
      log.warn("A lookup field must be formatted as ${[source seed].[source id]-[table seed].[table id]-[field name]}");
      return fieldName;
    }

    if (log.isDebugEnabled()) {
      log.debug("Attempting to find a values for [" + useValue + "]");
    }

    //
    // We check the context map first to make sure this row was not already loaded
    //
    String rowIdentifier = useValue.substring(0, useValue.lastIndexOf("-"));
    DataRow r = null;

    if (dataSet.get() != null) {
      r = dataSet.get().get(rowIdentifier);
    }
    if (r == null) {
      DataSource<String, DataTable> dS = providerMap.get(lookupMap[0]);
      if (dS == null) {
        log.warn("No data source was registered for [" + lookupMap[0] + "]");
        return fieldName;
      }

      DataTable<String, DataField, DataRow> h = dS.getTable(lookupMap[1]);
      if (h == null) {
        log.warn("No table was found for [" + lookupMap[1] + "] in [" + lookupMap[0] + "]");
        return fieldName;
      }

      if (log.isDebugEnabled()) {
        log.debug("The value was not found in the local map.  Loading a new row");
      }
      r = h.getRow(h.isLockable(), 120);
    }

    if (r == null) {
      log.warn("No data source was found for [" + lookupMap[1] + "] in [" + lookupMap[0] + "]");
      return fieldName;
    }

    //
    // Store a reference to the current row
    //
    dataSet.get().put(rowIdentifier, r);

    return r.getField(lookupMap[2]) + "";
  }

  /**
   * Returns a value in STRING form. This value is mapped by 2 parts separated
   * by -. *{[secrets provider seed].[secrets provider id]-[field name]}
   *
   * @param fieldName
   * @param contextMap
   * @return
   */
  private String getEnvironment(String fieldName, Map<String, Object> contextMap) {
    String useValue = fieldName;

   if (useValue.startsWith("%{")) {
      useValue = useValue.substring(2, useValue.length() - 1);
    }

    //
    // Before we use secrets, we check if a context variable or system property override this
    //
    Object returnValue = getContext(useValue, contextMap);
    if (returnValue != null) {
      return returnValue + "";
    }

    returnValue = environmentMap.get(useValue);

    if (returnValue == null) {
      return defaultEnvironmentMap.get(useValue);
    } else {
      return returnValue + "";
    }
  }

  /**
   * Returns a data table referenced by the given name
   * @param fieldName
   * @return
   */
  @Override
  public DataTable getTable(String fieldName) {

    if (fieldName == null || fieldName.trim().isEmpty()) {
      return null;
    }

    if (log.isDebugEnabled()) {
      log.debug("Attempting to find a values for [" + fieldName + "]");
    }

    String[] lookupMap = fieldName.split("-");

    if (lookupMap.length != 2) {
      throw new IllegalArgumentException("A data source lookup must be formatted as [source seed].[source id]-[table seed].[table id]");
    }

    DataSource<String, com.orasi.datasource.DataTable> dS = providerMap.get(lookupMap[0]);
    if (dS == null) {
      throw new IllegalArgumentException("No data source was registered for [" + lookupMap[0] + "]");
    }

    com.orasi.datasource.DataTable h = dS.getTable(lookupMap[1]);

    if (h == null) {
      throw new IllegalArgumentException("No table was found for [" + lookupMap[1] + "] in [" + lookupMap[0] + "]");
    }

    h.setKey(fieldName);

    return h;
  }
}
