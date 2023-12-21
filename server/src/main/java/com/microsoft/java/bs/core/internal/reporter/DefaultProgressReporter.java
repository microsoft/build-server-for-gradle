// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import java.util.UUID;

import com.microsoft.java.bs.core.Launcher;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskId;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import ch.epfl.scala.bsp4j.TaskStartParams;

/**
 * A default implementation of {@link ProgressReporter}.
 */
public class DefaultProgressReporter implements ProgressReporter {

  private final TaskId taskId;
  private BuildClient client;

  public DefaultProgressReporter() {
    this.taskId = new TaskId(UUID.randomUUID().toString());
    client = Launcher.client;
  }

  public void cleanUp() {
    client = null;
  }

  @Override
  public void taskStarted(String taskPath, String message, long startTime) {
    BuildClient localClient = client;
    if (localClient != null) {
      TaskStartParams startParam = new TaskStartParams(taskId);
      startParam.setEventTime(startTime);
      startParam.setMessage(message);
      localClient.onBuildTaskStart(startParam);
    }
  }

  @Override
  public void taskInProgress(String taskPath, String message, long startTime) {
    BuildClient localClient = client;
    if (localClient != null) {
      TaskProgressParams progressParam = new TaskProgressParams(taskId);
      progressParam.setEventTime(startTime);
      progressParam.setMessage(message);
      localClient.onBuildTaskProgress(progressParam);
    }
  }

  @Override
  public void taskFinished(String taskPath, String message,
      long startTime, StatusCode statusCode) {
    BuildClient localClient = client;
    if (localClient != null) {
      TaskFinishParams endParam = new TaskFinishParams(taskId, statusCode);
      endParam.setEventTime(startTime);
      endParam.setMessage(message);
      localClient.onBuildTaskFinish(endParam);
    }
  }
}
