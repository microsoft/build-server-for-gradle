// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import org.gradle.tooling.events.FailureResult;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationResult;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;
import org.gradle.tooling.events.StartEvent;
import org.gradle.tooling.events.task.TaskOperationDescriptor;

import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Implements {@link ProgressListener} that listens to the progress of gradle tasks,
 * and reports the progress to the client.
 */
public class TaskProgressReporter implements ProgressListener {

  private final ProgressReporter reporter;

  public TaskProgressReporter(ProgressReporter reporter) {
    this.reporter = reporter;
  }

  @Override
  public void statusChanged(ProgressEvent event) {
    String taskPath = null;
    if (event.getDescriptor() instanceof TaskOperationDescriptor) {
      TaskOperationDescriptor descriptor = (TaskOperationDescriptor) event.getDescriptor();
      taskPath = descriptor.getTaskPath();
    }
    if (event instanceof StartEvent) {
      taskStarted(taskPath, event.getDisplayName(), event.getEventTime());
    } else if (event instanceof FinishEvent) {
      OperationResult result = ((FinishEvent) event).getResult();
      StatusCode status = result instanceof FailureResult ? StatusCode.ERROR : StatusCode.OK;
      taskFinished(taskPath, event.getDisplayName(), event.getEventTime(), status);
    } else {
      taskInProgress(taskPath, event.getDisplayName(), event.getEventTime());
    }
  }

  public void taskStarted(String taskPath, String message, long eventTime) {
    reporter.taskStarted(taskPath, message, eventTime);
  }

  public void taskInProgress(String taskPath, String message, long eventTime) {
    reporter.taskInProgress(taskPath, message, eventTime);
  }

  public void taskFinished(String taskPath, String message, long eventTime, StatusCode statusCode) {
    reporter.taskFinished(taskPath, message, eventTime, statusCode);
  }
}
