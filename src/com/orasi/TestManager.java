package com.orasi;

import com.orasi.alchemy.mediation.execution.FunctionExecutionMediator;
import com.orasi.alchemy.mediation.execution.FunctionExecutor;
import com.orasi.alchemy.mediation.execution.StackableContext;
import com.orasi.datasource.DataField;
import com.orasi.datasource.DataRow;
import com.orasi.datasource.DataSourceProvider;
import com.orasi.datasource.DataSourceProviderFactory;
import com.orasi.datasource.DataTable;
import com.orasi.event.spi.StepEvent;
import com.orasi.model.StepPayload;
import static com.orasi.shared_library.getStepCounter;
import static com.orasi.shared_library.notifyListeners;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openqa.selenium.WebDriver;

public class TestManager implements FunctionExecutor {

  private static final Logger log = LoggerFactory.getLogger(TestManager.class);
  private static final TestManager singleton = new TestManager();

  private TestManager() {
    FunctionExecutionMediator.instance().setFunctionExecutor(this);
  }

  public List<TestWrapper> getTests() {
    return testList;
  }

  public List<String> getTestNames() {
    List<String> testNames = new ArrayList<>(50);
    testList.forEach((t) -> {
      testNames.add(t.getName());
    });
    return testNames;
  }

  public static TestManager instance() {
    return singleton;
  }

  private final Map<Integer, List<Integer>> synchronizationMap = new HashMap<>(10);

  private final Map<EndpointDevice, List<TestWrapper>> endpointMap = new HashMap<>(10);

  private final List<TestWrapper> testList = new ArrayList<>(10);

  private final Map<String, TestWrapper> testMap = new HashMap<>(10);

  public int getSize() {
    return testList.size();
  }

  private boolean hasTag(String[] globalTagList, String[] testTagList) {
    //
    // Do we have any tags defined?
    //
    if (globalTagList == null || globalTagList.length == 0) {
      return true;
    }

    //
    // We do, now does our test have any?
    //
    if (testTagList == null || testTagList.length == 0) {
      return false;
    }

    for (String g : globalTagList) {
      for (String t : testTagList) {
        if (g.trim().equalsIgnoreCase(t.trim())) {
          return true;
        }
      }
    }

    return false;

  }

  public void setTags(String[] tagList) {

    log.info("Tags were supplied - reconfiguring final test list");
    List<TestWrapper> finalList = new ArrayList<>(10);

    testList.stream().filter(tW -> (hasTag(tagList, tW.getTags()))).forEachOrdered(tW -> {
      log.info("Adding Tagged Test " + tW.getName());
      finalList.add(tW);
    });

    testList.clear();
    testList.addAll(finalList);
  }

  public void registerSynchronizationPoint(int pointId, List<Integer> testIds) {
    synchronizationMap.put(pointId, testIds);
  }

  public void registerFunction(TestWrapper tW) {
    //
    // Functions dont get added to the test list as they are executed on demand
    //
    testMap.put(tW.getAlchemyIdentifier(), tW);
  }

  @Override
  public void executeFunction(String alchemyIdentifier, int executionId, int testExecutionId, Object webDriver, Map<String, Object> variableMap, Map<String, Object> contextMap, String contextName, Stack<String> callStack, Stack<Integer> stepStack) {
    TestWrapper tW = testMap.get(alchemyIdentifier);
    if (tW == null) {
      throw new IllegalArgumentException("Could not locate test for " + alchemyIdentifier);
    }

    boolean addStep = false;
    String actionName = variableMap.get("__actionName") + "";
    String testName = variableMap.get( "__testName") + "" ;
    if (variableMap.containsKey("__addStep")) {
      addStep = Boolean.parseBoolean(variableMap.get("__addStep") + "");
    }

    Map<String, Object> ingressMap = null;

    if (variableMap.containsKey("ingressSignature")) {
      ingressMap = new HashMap<>(5);
      Map<String, FunctionVariableWrapper> ingressSignature = (Map) variableMap.get("ingressSignature");
      DataSourceProvider dS = (DataSourceProvider) variableMap.get("__dS");

      if (dS == null) {
        log.atWarn().log("A DataSourceProvider was no passed in using the __dS variable.  The default will be used.");
        dS = DataSourceProviderFactory.instance().getDataSourceProvider();
      }

      for (Map.Entry<String, FunctionVariableWrapper> mE : ingressSignature.entrySet()) {

        switch (mE.getValue().getType()) {
          case 1:
            ingressMap.put(mE.getKey(), dS.replaceValues(mE.getValue().getName(), contextMap) + "");
            break;
          case 2:
            ingressMap.put(mE.getKey(), parseNumber(dS.replaceValues(mE.getValue().getName(), contextMap) + "", Long.class));
            break;
          case 3:
            ingressMap.put(mE.getKey(), parseNumber(dS.replaceValues(mE.getValue().getName(), contextMap) + "", Double.class));
            break;
          case 4:
            ingressMap.put(mE.getKey(), dS.replaceValues(mE.getValue().getName(), contextMap) + "");
            break;
          case 5:
            ingressMap.put(mE.getKey(), ObjectManager.instance().getObject(dS.replaceValues(mE.getValue().getName(), contextMap), contextMap, dS));
            break;
          case 8:
            ingressMap.put(mE.getKey(), ((WebDriver) webDriver).findElement(ObjectManager.instance().getObject(dS.replaceValues(mE.getValue().getName(), contextMap), contextMap, dS)));
            break;
          case 9:
            ingressMap.put(mE.getKey(), dS.getTable(dS.replaceValues(mE.getValue().getName(), contextMap) + ""));
            break;
          case 10:
            ingressMap.put(mE.getKey(), dS.replaceValues(mE.getValue().getName(), contextMap) + "");
            break;
          default:
            ingressMap.put(mE.getKey(), dS.replaceValues(mE.getValue().getName(), contextMap) + "");
            break;
        }
      }
    }

    if (contextMap instanceof StackableContext) {
      ((StackableContext) contextMap).push();
    }

    if (contextMap != null) {
      if (variableMap.containsKey("functionData")) {
        contextMap.putAll((Map) variableMap.get("functionData"));
      }

      if (ingressMap != null && !ingressMap.isEmpty()) {
        contextMap.putAll((Map) ingressMap);
      }
    }
    
    StepPayload stepPayload = new StepPayload();
    int stepIdentifier = getStepCounter();
    
    try {

      if (addStep) {
        stepPayload.setActionName(actionName);
        stepPayload.setExecutionId(executionId);
        stepPayload.setStepId(stepIdentifier);
        stepPayload.setTestExecutionId(testExecutionId);
        stepPayload.setParentStep(0);
        stepStack.push(stepIdentifier);
        stepPayload.setStepDetail("{\"variableList\": [],\"functionVariables\": [],\"status\": 1,\"invertResult\": false,\"actionDisplay\": \"" + actionName + " by calling " + tW.getName() + "\"}");
        notifyListeners(new StepEvent(stepPayload, testName, 1));
      }
      tW.executeTest(executionId, testExecutionId, (WebDriver) webDriver, contextMap, contextName, callStack, stepStack);

      if (addStep) {
        notifyListeners(new StepEvent(stepPayload, testName, 4));
      }

    } catch (Throwable t) {
      if (addStep) {
        notifyListeners(new StepEvent(stepPayload, testName, 3));
      }
      
      throw new RuntimeException( "Failed to execute " + actionName, t );
    } finally {
      if ( addStep ) {
        stepStack.pop();
      }
      if (contextMap instanceof StackableContext) {
        ((StackableContext) contextMap).pop();
      }
    }
  }
  public void registerTest(TestWrapper tW) {

    testMap.put(tW.getAlchemyIdentifier(), tW);

    if (tW.getDataName() != null && !tW.getDataName().trim().isEmpty()) {

      DataTable<String, DataField, DataRow> dT = DataManager.instance().getTable(tW.getDataName());

      if (dT != null) {

        for (int i = 0; i < dT.getRows().size(); i++) {
          DataTestWrapper dW = new DataTestWrapper(tW, i, dT.getRows().get(i), tW.getDataName());
          log.info("Registering DATA wrapped test for " + tW.getName());
          testList.add(dW);
        }
      } else {
        log.info("Registering test as " + tW.getName());
        testList.add(tW);
      }
    } else {
      log.info("Registering test as " + tW.getName());
      testList.add(tW);
    }
  }

  public TestWrapper getTest(String alchemyIdentifier) {
    return testMap.get(alchemyIdentifier);
  }

  private <T extends Number> T parseNumber(Object value, Class<T> numberType) {
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

}
