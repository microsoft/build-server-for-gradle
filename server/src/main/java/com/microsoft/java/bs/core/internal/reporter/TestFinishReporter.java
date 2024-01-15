// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import java.util.UUID;

import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationResult;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;
import org.gradle.tooling.events.StartEvent;
import org.gradle.tooling.events.test.JvmTestOperationDescriptor;
import org.gradle.tooling.events.test.TestFailureResult;
import org.gradle.tooling.events.test.TestSkippedResult;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskId;
import ch.epfl.scala.bsp4j.TaskStartParams;
import ch.epfl.scala.bsp4j.TestFinish;
import ch.epfl.scala.bsp4j.TestStart;
import ch.epfl.scala.bsp4j.TestStatus;

/**
 * Implements {@link ProgressListener} to record test results.
 * Individual test reports. 
 */
public class TestFinishReporter implements ProgressListener {

  private final TaskId taskId;
  private final BuildClient client;

  public TestFinishReporter(BuildClient client) {
    this.taskId = new TaskId(UUID.randomUUID().toString());
    this.client = client;
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
        } else if (event instanceof FinishEvent) {
          OperationResult result = ((FinishEvent) event).getResult();
          StatusCode statusCode = StatusCode.OK;
          TestStatus testStatus = TestStatus.PASSED;
          if (result instanceof TestFailureResult) {
            statusCode = StatusCode.ERROR;
            testStatus = TestStatus.FAILED;
          } else if (result instanceof TestSkippedResult) {
            testStatus = TestStatus.SKIPPED;
          }
          TaskFinishParams finishParam = new TaskFinishParams(taskId, statusCode);
          finishParam.setMessage("Finish test");
          finishParam.setDataKind("test-finish");
          finishParam.setEventTime(event.getEventTime());
          TestFinish testFinish = new TestFinish(event.getDisplayName(), testStatus);
          finishParam.setData(testFinish);
          client.onBuildTaskFinish(finishParam);
        }
      }
    }
  }
}
