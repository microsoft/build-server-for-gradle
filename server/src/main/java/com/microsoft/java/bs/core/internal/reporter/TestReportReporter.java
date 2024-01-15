// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import java.util.UUID;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationResult;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;
import org.gradle.tooling.events.StartEvent;
import org.gradle.tooling.events.test.JvmTestOperationDescriptor;
import org.gradle.tooling.events.test.TestFailureResult;
import org.gradle.tooling.events.test.TestSkippedResult;
import org.gradle.tooling.events.test.TestSuccessResult;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskId;
import ch.epfl.scala.bsp4j.TaskStartParams;
import ch.epfl.scala.bsp4j.TestReport;
import ch.epfl.scala.bsp4j.TestStart;

/**
 * Implements {@link ProgressListener} to record test results.
 * Test summary report. e.g. number of fails.
 */
public class TestReportReporter implements ProgressListener {

  private final TaskId taskId;
  private final BuildTargetIdentifier btId;
  private final BuildClient client;
  private int successCount;
  private int skippedCount;
  private int failureCount;
  private Long latestTime;
  private RuntimeException exception;

  /**
   * initialise.
   *
   * @param btId the build target being tested.
   */
  public TestReportReporter(BuildTargetIdentifier btId, BuildClient client) {
    this.taskId = new TaskId(UUID.randomUUID().toString());
    this.btId = btId;
    this.client = client;
    successCount = 0;
    skippedCount = 0;
    failureCount = 0;
    latestTime = null;
    exception = null;
  }

  @Override
  public void statusChanged(ProgressEvent event) {
    if (event.getDescriptor() instanceof JvmTestOperationDescriptor) {
      // only report on methods
      JvmTestOperationDescriptor descriptor = (JvmTestOperationDescriptor) event.getDescriptor();
      if (descriptor.getClassName() != null && descriptor.getMethodName() != null) {
        if (event instanceof StartEvent) {
          TaskStartParams startParam = new TaskStartParams(taskId);
          startParam.setMessage("Start test");
          startParam.setDataKind("test-start");
          startParam.setEventTime(event.getEventTime());
          TestStart testStart = new TestStart(event.getDisplayName());
          startParam.setData(testStart);
          client.onBuildTaskStart(startParam);
          latestTime = event.getEventTime();
        } else if (event instanceof FinishEvent) {
          OperationResult result = ((FinishEvent) event).getResult();
          if (result instanceof TestSuccessResult) {
            successCount += 1;
          } else if (result instanceof TestSkippedResult) {
            skippedCount += 1;
          } else if (result instanceof TestFailureResult) {
            failureCount += 1;
          }
          latestTime = event.getEventTime();
        }
      }
    }
  }

  /**
   * Add any exception not dealt with by the progress events.
   *
   * @param e exception in test run
   */
  public void addException(RuntimeException e) {
    // just report a single exception as it must be an issue with a connection to Gradle
    // or an API support issue because of Gradle version
    // Individual test exceptions are reported elsewhere.
    exception = e;
  }

  /**
   * send the test summary back to the BSP client.
   */
  public void sendResult() {
    StatusCode statusCode = StatusCode.OK;
    if (failureCount > 0 || exception != null) {
      statusCode = StatusCode.ERROR;
    }
    TaskFinishParams finishParam = new TaskFinishParams(taskId, statusCode);
    if (exception != null) {
      String message = String.join("\n", ExceptionUtils.getRootCauseStackTraceList(exception));
      finishParam.setMessage("Exception in tests " + message);
    } else {
      finishParam.setMessage("Finish test");
    }
    finishParam.setDataKind("test-report");
    finishParam.setEventTime(latestTime);
    TestReport testFinish = new TestReport(btId, successCount, failureCount, 0, 0, skippedCount);
    finishParam.setData(testFinish);
    client.onBuildTaskFinish(finishParam);
  }
}
