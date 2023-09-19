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

  @Override
  public void taskStarted(String message) {
    TaskStartParams startParam = new TaskStartParams(taskId);
    startParam.setMessage(message);
    if (client != null) {
      client.onBuildTaskStart(startParam);
    }
  }

  @Override
  public void taskInProgress(String message) {
    TaskProgressParams progressParam = new TaskProgressParams(taskId);
    progressParam.setMessage(message);
    if (client != null) {
      client.onBuildTaskProgress(progressParam);
    }
  }

  @Override
  public void taskFinished(String message, StatusCode statusCode) {
    TaskFinishParams endParam = new TaskFinishParams(taskId, statusCode);
    endParam.setMessage(message);
    if (client != null) {
      client.onBuildTaskFinish(endParam);
    }
    client = null;
  }
}
