// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import ch.epfl.scala.bsp4j.TaskId;
import ch.epfl.scala.bsp4j.TaskStartParams;
import ch.epfl.scala.bsp4j.TestStatus;
import ch.epfl.scala.bsp4j.extended.TestFinishEx;
import ch.epfl.scala.bsp4j.extended.TestName;
import ch.epfl.scala.bsp4j.extended.TestStartEx;

import java.util.ArrayList;
import java.util.List;

import org.gradle.tooling.TestFailure;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationDescriptor;
import org.gradle.tooling.events.OperationResult;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.StartEvent;
import org.gradle.tooling.events.test.JvmTestKind;
import org.gradle.tooling.events.test.JvmTestOperationDescriptor;
import org.gradle.tooling.events.test.TestFailureResult;
import org.gradle.tooling.events.test.TestSkippedResult;
import org.gradle.tooling.events.test.TestSuccessResult;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TestReport;

/**
 * Implements {@link ProgressReporter} to record test results.
 * Test summary report. e.g. number of fails.
 */
public class TestReportReporter extends ProgressReporter {

  private final BuildTargetIdentifier btId;
  private int successCount;
  private int skippedCount;
  private int failureCount;
  private String exception;
  private long testDuration;

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
    exception = null;
    testDuration = 0;
  }

  private TestName getTestName(JvmTestOperationDescriptor eventDescriptor) {
    List<JvmTestOperationDescriptor> fullStack = new ArrayList<>();
    fullStack.add(eventDescriptor);
    OperationDescriptor descriptor = eventDescriptor.getParent();
    while (descriptor != null) {
      if (descriptor instanceof JvmTestOperationDescriptor jvmTestOperationDescriptor) {
        fullStack.add(jvmTestOperationDescriptor);
      }
      descriptor = descriptor.getParent();
    }
    // Gradle can have blank classnames on dynamic tests even though the test is still
    // within the class, so search until classname disappears completely.
    int i = fullStack.size() - 1;
    boolean classNameFound = false;
    while (i >= 0 && !classNameFound) {
      if (fullStack.get(i).getClassName() != null) {
        classNameFound = true;
      } else {
        i--;
      }
    }
    // earlier check means that classname will always be found so can't have i < 0
    // reverse list order
    TestName testName = null;
    while (i >= 0) {
      JvmTestOperationDescriptor desc = fullStack.get(i);
      String displayName;
      try {
        displayName = desc.getTestDisplayName();
      } catch (NoSuchMethodError e) {
        displayName = desc.getDisplayName();
      }
      TestName currentTestName = new TestName(displayName, desc.getSuiteName(),
          desc.getClassName(), desc.getMethodName());
      currentTestName.setParent(testName);
      testName = currentTestName;
      i--;
    }
    return testName;
  }

  @Override
  public void statusChanged(ProgressEvent event) {
    if (client != null) {
      if (event.getDescriptor() instanceof JvmTestOperationDescriptor descriptor) {
        TestName testName = getTestName(descriptor);
        // do not send reports on Gradle internal test tasks
        if (testName != null) {
          String taskPath = getTaskPath(event.getDescriptor());
          TaskId taskId = getTaskId(taskPath);
          if (event instanceof StartEvent) {
            TaskStartParams startParam = new TaskStartParams(taskId);
            startParam.setMessage("Start test");
            startParam.setDataKind("test-start");
            startParam.setEventTime(event.getEventTime());
            TestStartEx testStart = new TestStartEx(event.getDisplayName(), testName);
            startParam.setData(testStart);
            client.onBuildTaskStart(startParam);
          } else if (event instanceof FinishEvent finishEvent) {
            OperationResult result = finishEvent.getResult();
            testDuration += result.getEndTime() - result.getStartTime();
            StatusCode statusCode = StatusCode.OK;
            TestStatus testStatus = TestStatus.PASSED;
            String stackTrace = null;
            if (result instanceof TestFailureResult testFailureResult) {
              statusCode = StatusCode.ERROR;
              testStatus = TestStatus.FAILED;
              stackTrace = testFailureResult.getFailures()
                  .stream()
                  .filter(f -> f instanceof TestFailure)
                  .map(f -> (TestFailure) f)
                  .map(TestFailure::getStacktrace)
                  .findFirst()
                  .orElse(null);
              if (descriptor.getJvmTestKind() == JvmTestKind.ATOMIC) {
                failureCount += 1;
              }
            } else if (result instanceof TestSkippedResult) {
              testStatus = TestStatus.SKIPPED;
              if (descriptor.getJvmTestKind() == JvmTestKind.ATOMIC) {
                skippedCount += 1;
              }
            } else if (result instanceof TestSuccessResult) {
              if (descriptor.getJvmTestKind() == JvmTestKind.ATOMIC) {
                successCount += 1;
              }
            }
            TaskFinishParams finishParam = new TaskFinishParams(taskId, statusCode);
            finishParam.setMessage("Finish test");
            finishParam.setDataKind("test-finish");
            finishParam.setEventTime(event.getEventTime());
            TestFinishEx testFinish = new TestFinishEx(event.getDisplayName(), testStatus,
                testName);
            testFinish.setStackTrace(stackTrace);
            finishParam.setData(testFinish);
            client.onBuildTaskFinish(finishParam);
          }
        }
      }
    }
  }

  /**
   * Add any exception not dealt with by the progress events.
   *
   * @param exception Exception message
   */
  public void addException(String exception) {
    // just report a single exception as it must be an issue with a connection to Gradle
    // or an API support issue because of Gradle version
    // Individual test exceptions are reported elsewhere.
    this.exception = exception;
  }

  /**
   * send the test summary back to the BSP client.
   */
  public void sendResult() {
    if (client != null) {
      TestReport testReport = new TestReport(btId, successCount, failureCount, 0, 0, skippedCount);
      testReport.setOriginId(originId);
      testReport.setTime(testDuration);
      StatusCode statusCode = StatusCode.OK;
      if (failureCount > 0 || exception != null) {
        statusCode = StatusCode.ERROR;
      }
      TaskFinishParams finishParam = new TaskFinishParams(taskId, statusCode);
      if (exception != null) {
        finishParam.setMessage("Exception in tests " + exception);
      } else {
        finishParam.setMessage("Finish test");
      }
      finishParam.setEventTime(System.currentTimeMillis());
      finishParam.setDataKind("test-report");
      finishParam.setData(testReport);
      client.onBuildTaskFinish(finishParam);
    }
  }
}
