// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import ch.epfl.scala.bsp4j.TaskId;
import ch.epfl.scala.bsp4j.TaskStartParams;
import ch.epfl.scala.bsp4j.TestFinish;
import ch.epfl.scala.bsp4j.TestStart;
import ch.epfl.scala.bsp4j.TestStatus;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationResult;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.StartEvent;
import org.gradle.tooling.events.test.JvmTestOperationDescriptor;
import org.gradle.tooling.events.test.TestFailureResult;
import org.gradle.tooling.events.test.TestSkippedResult;
import org.gradle.tooling.events.test.TestSuccessResult;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TestReport;
import org.gradle.tooling.internal.consumer.DefaultTestAssertionFailure;

/**
 * Implements {@link ProgressReporter} to record test results.
 * Test summary report. e.g. number of fails.
 */
public class TestReportReporter extends ProgressReporter {

  private final BuildTargetIdentifier btId;
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
  public TestReportReporter(BuildTargetIdentifier btId, BuildClient client, String originId) {
    super(client, originId);
    this.btId = btId;
    successCount = 0;
    skippedCount = 0;
    failureCount = 0;
    latestTime = null;
    exception = null;
  }

  @Override
  public void statusChanged(ProgressEvent event) {
    if (client != null) {
      if (event.getDescriptor() instanceof JvmTestOperationDescriptor) {
        // only report on methods
        JvmTestOperationDescriptor descriptor = (JvmTestOperationDescriptor) event.getDescriptor();
        if (descriptor.getClassName() != null && descriptor.getMethodName() != null) {
          TaskId taskId = getTaskId(descriptor.getDisplayName());
          if (event instanceof StartEvent) {
            TaskStartParams startParam = new TaskStartParams(taskId);
            startParam.setMessage("Start test");
            startParam.setDataKind("test-start");
            startParam.setEventTime(event.getEventTime());
            TestStart testStart = new TestStart(event.getDisplayName());
            // TODO Gradle does not provide file/position data
            startParam.setData(testStart);
            client.onBuildTaskStart(startParam);
          } else if (event instanceof FinishEvent) {
            OperationResult result = ((FinishEvent) event).getResult();
            StatusCode statusCode = StatusCode.OK;
            TestStatus testStatus = TestStatus.PASSED;
            String stackTrace = null;
            if (result instanceof TestFailureResult) {
              statusCode = StatusCode.ERROR;
              testStatus = TestStatus.FAILED;
              stackTrace = ((TestFailureResult) result).getFailures()
                  .stream()
                  .filter(f -> f instanceof DefaultTestAssertionFailure)
                  .map(f -> (DefaultTestAssertionFailure) f)
                  .map(DefaultTestAssertionFailure::getStacktrace)
                  .findFirst()
                  .orElse(null);
              failureCount += 1;
            } else if (result instanceof TestSkippedResult) {
              testStatus = TestStatus.SKIPPED;
              skippedCount += 1;
            } else if (result instanceof TestSuccessResult) {
              successCount += 1;
            }
            TaskFinishParams finishParam = new TaskFinishParams(taskId, statusCode);
            finishParam.setMessage("Finish test");
            finishParam.setDataKind("test-finish");
            finishParam.setEventTime(event.getEventTime());
            TestFinish testFinish = new TestFinish(event.getDisplayName(), testStatus);
            // TODO Gradle does not provide file/position data
            // TODO BSP does not offer a way to pass back stacktrace
            testFinish.setMessage(stackTrace);
            finishParam.setData(testFinish);
            client.onBuildTaskFinish(finishParam);
            latestTime = event.getEventTime();
          }
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
    if (client != null) {
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
}
