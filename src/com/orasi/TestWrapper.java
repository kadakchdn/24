package com.orasi;

import com.orasi.datasource.DataRow;
import java.util.Map;
import java.util.Stack;
import org.openqa.selenium.WebDriver;

public interface TestWrapper {

  public String getName();

  public String getDescription();

  public int getId();

  public String getDataName();

  public DataRow getData();

  public int getDataIndex();

  public int getSynchronizationPoint();

  public String[] getTags();

  public String getAlchemyIdentifier();

  public String getTestDetail();

  public void executeTest(int executionId, int testExecutionId, WebDriver webDriver);

  public void executeTest(int executionId, int testExecutionId, WebDriver webDriver, Map<String, Object> contextMap, String contextName, Stack<String> callStack, Stack<Integer> stepStack);
}
