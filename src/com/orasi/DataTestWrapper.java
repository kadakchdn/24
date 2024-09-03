package com.orasi;

import com.orasi.datasource.DataRow;
import java.util.Map;
import java.util.Stack;
import org.openqa.selenium.WebDriver;

/**
 * Allows a test to be wrapped and executed multiple times given a set of data
 * @author allen
 */
public class DataTestWrapper implements TestWrapper {

  private final TestWrapper baseTest;
  private final int dataIndex;
  private final DataRow data;
  private final String rowIdentifier;

  /**
   * The alchemy identifier of the base test
   * @return
   */
  @Override
  public String getAlchemyIdentifier() {
    return baseTest.getAlchemyIdentifier();
  }

  /**
   * Create a new data wrapped test
   * @param baseTest The base test instance
   * @param dataIndex The current data index of the row used for this execution
   * @param data The name/value set of row data
   * @param rowIdentifier The name of data table row identifier
   */
  public DataTestWrapper(TestWrapper baseTest, int dataIndex, DataRow data, String rowIdentifier ) {
    this.baseTest = baseTest;
    this.dataIndex = dataIndex;
    this.data = data;
    this.rowIdentifier = rowIdentifier;
  }

  /**
   * REturns a JOSN definition of the test
   * @return
   */
  @Override
  public String getTestDetail() {
    return baseTest.getTestDetail();
  }

  /**
   * Returns a list of test tags
   * @return
   */
  @Override
  public String[] getTags() {
    return baseTest.getTags();
  }

  /**
   * 
   * @param executionId
   * @param testExecutionId
   * @param webDriver
   */
  @Override
  public void executeTest(int executionId, int testExecutionId, WebDriver webDriver) {
    DataManager.instance().setRow( rowIdentifier, data);
    baseTest.executeTest(executionId, testExecutionId, webDriver);
    DataManager.instance().setRow(rowIdentifier, null);
  }

  /**
   *
   * @param executionId
   * @param testExecutionId
   * @param webDriver
   * @param contextMap
   * @param contextName
   * @param callStack
   * @param stepStack
   */
  @Override
  public void executeTest(int executionId, int testExecutionId, WebDriver webDriver, Map<String, Object> contextMap, String contextName, Stack<String> callStack, Stack<Integer> stepStack) {
    baseTest.executeTest(executionId, testExecutionId, webDriver, contextMap, contextName, callStack, stepStack);
  }

  /**
   *
   * @return
   */
  @Override
  public int getId() {
    return baseTest.getId();
  }

  /**
   *
   * @return
   */
  @Override
  public String getDescription() {
    return baseTest.getDescription();
  }

  /**
   *
   * @return
   */
  @Override
  public String getDataName() {
    return baseTest.getDataName();
  }

  /**
   *
   * @return
   */
  @Override
  public int getSynchronizationPoint() {
    return baseTest.getSynchronizationPoint();
  }

  /**
   *
   * @return
   */
  @Override
  public String getName() {
    if (data != null && data.getRawData() != null && !data.getRawData().isEmpty()) {
      
      if ( data.getRawData().containsKey( "name" ) ) {
        return baseTest.getName() + " (" + data.getRawData().get( "name" ) + ")_" + dataIndex;
      }
      else {
        return baseTest.getName() + " (" + data.getRawData().values().toArray()[0] + ")_" + dataIndex;
      }
    } else {
      return baseTest.getName() + "_" + dataIndex;
    }
  }

  /**
   *
   * @return
   */
  @Override
  public DataRow getData() {
    return data;
  }

  /**
   *
   * @return
   */
  @Override
  public int getDataIndex() {
    return dataIndex;
  }

}
