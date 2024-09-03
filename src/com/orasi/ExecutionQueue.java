package com.orasi;

import com.google.gson.Gson;
import com.orasi.event.spi.StepEvent;
import com.orasi.event.spi.TestEvent;
import com.orasi.model.EOTException;
import com.orasi.model.StepException;
import com.orasi.model.StepPayload;
import com.orasi.model.TestPayload;
import static com.orasi.shared_library.getStepCounter;
import static com.orasi.shared_library.notifyListeners;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author allen
 */
public class ExecutionQueue implements Runnable {

  private static final Gson gson = new Gson();
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final BlockingQueue<ExecutionWrapper> executionQueue = new LinkedBlockingQueue<>();
  private boolean valid = true;
  private final int threadCount;
  private final int failureRepeat;
  private final int targetFailureRepeat;
  private final List<Thread> threadList = new ArrayList<>();

  private final Map<String, AtomicInteger> targetAvailability = new HashMap<>(5);

  public ExecutionQueue(int threadCount, int failureRepeat, int targetFailureRepeat) {
    this.threadCount = threadCount;
    this.failureRepeat = failureRepeat;
    this.targetFailureRepeat = targetFailureRepeat;
  }

  public void addExecution(EndpointDevice eD, TestWrapper tW) {
    if (!targetAvailability.containsKey(eD.getExecutionTarget().getAlchemyIdentifier())) {
      targetAvailability.put(eD.getExecutionTarget().getAlchemyIdentifier(), new AtomicInteger(eD.getMaximumAvailable()));
    }

    log.atInfo().log("Registering " + tW.getName() + " to execute on " + eD.getName());
    executionQueue.offer(new ExecutionWrapper(this, eD, tW));
  }

  private synchronized boolean checkout(EndpointDevice eD) {
    AtomicInteger aI = targetAvailability.get(eD.getExecutionTarget().getAlchemyIdentifier());
    if (aI.intValue() > 0) {
      int devicesRemaining = aI.decrementAndGet();
      log.atInfo().log(devicesRemaining + " remaining for " + eD.getName() + " after checkout");
      return true;
    }

    return false;
  }

  public synchronized ExecutionWrapper pop() {
    ExecutionWrapper eW;
    try {
      while (!executionQueue.isEmpty()) {
        while ((eW = executionQueue.poll(5, TimeUnit.SECONDS)) != null) {
          if (checkout(eW.getEndpointDevice())) {
            return eW;
          } else {
            executionQueue.offer(eW);
          }
        }
      }
    } catch (InterruptedException e) {

    }

    return null;
  }

  public void checkin(EndpointDevice eD) {
    AtomicInteger aI = targetAvailability.get(eD.getExecutionTarget().getAlchemyIdentifier());
    int devicesRemaining = aI.addAndGet(1);
    log.atInfo().log(devicesRemaining + " remaining for " + eD.getName() + " after checkin");
  }

  @Override
  public void run() {
    for (int i = 0; i < threadCount; i++) {
      Thread t = new Thread(new Runner());
      threadList.add(t);
      t.start();
    }

    while (valid) {
      boolean isValid = false;

      for (Thread t : threadList) {
        if (t.isAlive()) {
          isValid = true;
        }
      }

      try {
        Thread.sleep(1000);
      } catch (InterruptedException ex) {

      }

      valid = isValid;
    }

  }

  private class Runner implements Runnable {

    @Override
    public void run() {

      while (valid) {
        ExecutionWrapper eW = pop();

        if (eW == null) {
          log.atWarn().log("Execution Thread existing - nothing more to do");
          break;
        }

        try {
          int testStatus = -1;
          MonitoredRemoteWebDriver wD = null;

          TestPayload testPayload = new TestPayload();
          testPayload.setExecutionIdentifier(SuiteExecutionWrapper.instance().getExecutionId());
          testPayload.setTestExecutionIdentifier(eW.getExecutionId());
          testPayload.setSuiteId(SuiteExecutionWrapper.instance().getId());
          testPayload.setTestDetail(eW.getTestWrapper().getTestDetail());
          testPayload.setTargetDetail(eW.getEndpointDevice().getTargetDetail());
          testPayload.setRouterDetail(eW.getEndpointDevice().getRouterDetail());
          notifyListeners(new TestEvent(testPayload, eW.getTestWrapper().getName(), 1));

          StepPayload stepPayload = new StepPayload();
          stepPayload.setActionName("Initialize");
          stepPayload.setExecutionId(SuiteExecutionWrapper.instance().getExecutionId());
          stepPayload.setStepId(getStepCounter());
          stepPayload.setTestExecutionId(eW.getExecutionId());
          stepPayload.setParentStep(0);
          stepPayload.setStepDetail("{'actionDisplay': 'Initializing test data...'}");

          try {
            notifyListeners(new StepEvent(stepPayload, eW.getTestWrapper().getName(), 1));
            DataManager.instance().initializeTest();
            stepPayload.setStatus(1);
            notifyListeners(new StepEvent(stepPayload, eW.getTestWrapper().getName(), 4));
          } catch (Throwable t) {
            stepPayload.setStatus(-1);
            stepPayload.setMessage(t.getMessage());
            notifyListeners(new StepEvent(stepPayload, eW.getTestWrapper().getName(), 3));
            throw t;
          }

          Map<String, String> varMap = new HashMap<>(10);
          varMap.putAll(eW.getEndpointDevice().getRouter().getPropertyMap());
          varMap.putAll(eW.getEndpointDevice().getExecutionTarget().getPropertyMap());
          String varDetail = gson.toJson(varMap);

          try {
            wD = new MonitoredRemoteWebDriver((RemoteWebDriver) eW.getEndpointDevice().connect(eW.getTestWrapper(), eW.getExecutionId()), eW);
          } catch (Exception e) {
            if (eW.incrementTargetFailureCount() < targetFailureRepeat) {
              notifyListeners(new TestEvent(testPayload, eW.getTestWrapper().getName(), 6));
              executionQueue.add(eW);
            } else {

              testPayload = new TestPayload();
              testPayload.setMessage(e.getMessage());
              testPayload.setTestExecutionIdentifier(eW.getExecutionId());
              testPayload.setFailureType(1);

              notifyListeners(new TestEvent(testPayload, eW.getTestWrapper().getName(), 3));
            }
            continue;
          }

          try {

            //
            // Execute the test
            //
            eW.getTestWrapper().executeTest(SuiteExecutionWrapper.instance().getExecutionId(), eW.getExecutionId(), wD);

            testPayload = new TestPayload();
            testPayload.setTestExecutionIdentifier(eW.getExecutionId());
            testStatus = 4;

          } catch (EOTException e) {
            testPayload = new TestPayload();
            testPayload.setMessage(e.getMessage());
            testPayload.setTestExecutionIdentifier(eW.getExecutionId());
            testPayload.setFailureType(2);
            if (e.isTestPassed()) {
              testStatus = 4;
            } else {
              log.error(eW.getTestWrapper().getClass().getName() + "[" + eW.getExecutionId() + "] on " + eW.getEndpointDevice().getName() + " failed with" + e.getMessage(), e);
              testStatus = 3;
            }
          } catch (StepException t) {
            log.error(eW.getTestWrapper().getClass().getName() + "[" + eW.getExecutionId() + "] on " + eW.getEndpointDevice().getName() + " failed with" + t.getMessage(), t);
            testPayload = new TestPayload();
            testPayload.setMessage(t.getMessage());
            testPayload.setTestExecutionIdentifier(eW.getExecutionId());
            testPayload.setFailureType(t.getFailureType().getId());
            testStatus = 3;
          } catch (Throwable t) {
            testPayload = new TestPayload();
            if (t.getCause() != null) {
              log.error(eW.getTestWrapper().getClass().getName() + "[" + eW.getExecutionId() + "] on " + eW.getEndpointDevice().getName() + " failed with" + t.getCause().getMessage(), t);
              testPayload.setMessage(t.getCause().getMessage());
            } else {
              log.error(eW.getTestWrapper().getClass().getName() + "[" + eW.getExecutionId() + "] on " + eW.getEndpointDevice().getName() + " failed with" + t.getMessage(), t);
              testPayload.setMessage(t.getMessage());
            }
            testPayload.setFailureType(4);
            testPayload.setTestExecutionIdentifier(eW.getExecutionId());
            testStatus = 3;
          } finally {
            if (log.isInfoEnabled()) {
              log.info("Test Complete!");
            }

            stepPayload = new StepPayload();
            stepPayload.setActionName("Remove Data");
            stepPayload.setExecutionId(SuiteExecutionWrapper.instance().getExecutionId());
            stepPayload.setStepId(getStepCounter());
            stepPayload.setTestExecutionId(eW.getExecutionId());
            stepPayload.setParentStep(0);
            stepPayload.setStepDetail("{'actionDisplay': 'Cleaning up test data...'}");
            notifyListeners(new StepEvent(stepPayload, eW.getTestWrapper().getName(), 1));
            DataManager.instance().afterTest();
            stepPayload.setStatus(1);
            notifyListeners(new StepEvent(stepPayload, eW.getTestWrapper().getName(), 4));

            try {
              stepPayload = new StepPayload();
              stepPayload.setActionName("Quit");
              stepPayload.setExecutionId(SuiteExecutionWrapper.instance().getExecutionId());
              stepPayload.setStepId(getStepCounter());
              stepPayload.setTestExecutionId(eW.getExecutionId());
              stepPayload.setParentStep(0);
              stepPayload.setVariableList(varDetail);
              stepPayload.setStepDetail("{'actionDisplay': 'Disconnecting session for {var:browserName} from {var:URL}'}");
              notifyListeners(new StepEvent(stepPayload, eW.getTestWrapper().getName(), 1));
              wD.quit();
              stepPayload.setStatus(1);
              notifyListeners(new StepEvent(stepPayload, eW.getTestWrapper().getName(), 4));
            } catch (Exception e) {
              stepPayload.setStatus(-1);
              stepPayload.setMessage(e.getMessage());
              notifyListeners(new StepEvent(stepPayload, eW.getTestWrapper().getName(), 3));
            }

            if (testStatus == 4) {
              notifyListeners(new TestEvent(testPayload, eW.getTestWrapper().getName(), testStatus));
            } else {
              if (eW.getTargetFailureCount() < targetFailureRepeat && eW.incrementFailureCount() < failureRepeat) {
                notifyListeners(new TestEvent(testPayload, eW.getTestWrapper().getName(), 6));
                executionQueue.add(eW);
              } else {
                notifyListeners(new TestEvent(testPayload, eW.getTestWrapper().getName(), testStatus));
              }
            }

            Thread.currentThread().setName("Idle Thread...");
          }

        } finally {
          checkin(eW.getEndpointDevice());
        }

      }

    }

  }

}
