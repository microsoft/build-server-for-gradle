// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import org.gradle.tooling.events.FailureResult;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationResult;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.StartEvent;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskId;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import ch.epfl.scala.bsp4j.TaskStartParams;

/**
 * A default implementation of {@link ProgressReporter}.
 */
public class AppRunReporter extends ProgressReporter {

  private final String taskName;

  /**
   * Instantiates a {@link AppRunReporter}.
   *
   * @param client BSP client to report to.
   * @param taskName the name of the task that runs the app.
   */
  public AppRunReporter(BuildClient client, String taskName) {
    super(client, null);
    this.taskName = taskName;
  }

  @Override
  public void statusChanged(ProgressEvent event) {
    if (client != null) {
      if (event.toString().contains(taskName)) {
        String taskPath = getTaskPath(event.getDescriptor());
        TaskId taskId = getTaskId(taskPath);
        if (event instanceof StartEvent) {
          taskStarted(taskId, event.getDisplayName());
        } else if (event instanceof FinishEvent) {
          OperationResult result = ((FinishEvent) event).getResult();
          StatusCode status = result instanceof FailureResult ? StatusCode.ERROR : StatusCode.OK;
          taskFinished(taskId, event.getDisplayName(), status);
        } else {
          taskInProgress(taskId, event.getDisplayName());
        }
      }
    }
  }

  private void taskStarted(TaskId taskId, String message) {
    TaskStartParams startParam = new TaskStartParams(taskId);
    startParam.setEventTime(System.currentTimeMillis());
    startParam.setMessage(message);
    client.onBuildTaskStart(startParam);
  }

  private void taskInProgress(TaskId taskId, String message) {
    TaskProgressParams progressParam = new TaskProgressParams(taskId);
    progressParam.setEventTime(System.currentTimeMillis());
    progressParam.setMessage(message);
    client.onBuildTaskProgress(progressParam);
  }

  private void taskFinished(TaskId taskId, String message,
      StatusCode statusCode) {
    TaskFinishParams endParam = new TaskFinishParams(taskId, statusCode);
    endParam.setEventTime(System.currentTimeMillis());
    endParam.setMessage(message);
    client.onBuildTaskFinish(endParam);
  }
}
