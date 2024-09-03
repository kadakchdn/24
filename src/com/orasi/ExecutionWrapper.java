package com.orasi;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.orasi.shared_library.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ExecutionWrapper {

  
  private final Logger log = LoggerFactory.getLogger(ExecutionWrapper.class);
  private final EndpointDevice eD;
  private final TestWrapper tW;
  private long startTime;
  private long lastAccess = System.currentTimeMillis();
  private int testExecutionId = getTestCounter();
  
  private final AtomicInteger targetFailureCount = new AtomicInteger(0);
  private final AtomicInteger failureCount = new AtomicInteger(0);
  
  private final ExecutionQueue eQ;

  public ExecutionWrapper( ExecutionQueue eQ, EndpointDevice eD, TestWrapper tW ) {
    this.eD = eD;
    this.tW = tW;
    this.eQ = eQ;
  }
  
  public int getTargetFailureCount() {
    return targetFailureCount.get();
  }
  
  public int incrementTargetFailureCount() {
    return targetFailureCount.addAndGet(1);
  }
  
  public int incrementFailureCount() {
    return failureCount.addAndGet(1);
  }

  public long getRunTime() {
    return System.currentTimeMillis() - startTime;
  }
  
  public void start() {
    startTime = System.currentTimeMillis();
  }
  
  public void reset() {
    testExecutionId = getTestCounter();
  }
  
  public int getExecutionId() {
    return testExecutionId;
  }

  protected List<Throwable> getThrowableList(Throwable throwable) {
    final List<Throwable> list = new ArrayList<>();
    while (throwable != null && !list.contains(throwable)) {
      list.add(throwable);
      throwable = throwable.getCause();
    }
    return list;
  }

  protected Throwable getRootCause(final Throwable throwable) {
    final List<Throwable> list = getThrowableList(throwable);
    return list.isEmpty() ? throwable : list.get(list.size() - 1);
  }

  /**
   * @return the lastAccess
   */
  public long getLastAccess() {
    return lastAccess;
  }

  public void setLastAccess() {
    this.lastAccess = System.currentTimeMillis();
  }

  /**
   * @return the eD
   */
  public EndpointDevice getEndpointDevice() {
    return eD;
  }

  /**
   * @return the tW
   */
  public TestWrapper getTestWrapper() {
    return tW;
  }

  public int getId() {
    return testExecutionId;
  }

}
