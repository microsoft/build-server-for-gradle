// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import java.util.UUID;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CompileReport;
import ch.epfl.scala.bsp4j.CompileTask;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskDataKind;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskId;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import ch.epfl.scala.bsp4j.TaskStartParams;

/**
 * An implementation of {@link ProgressReporter} used for compilation tasks.
 */
public class CompileProgressReporter implements ProgressReporter {

  private final BuildTargetIdentifier btId;
  private final TaskId taskId;
  private final BuildClient client;

  /**
   * Instantiates a {@link CompileProgressReporter}.
   *
   * @param btId Build target identifier
   */
  public CompileProgressReporter(BuildTargetIdentifier btId, BuildClient client) {
    this.btId = btId;
    this.taskId = new TaskId(UUID.randomUUID().toString());
    this.client = client;
  }

  @Override
  public void taskStarted(String message) {
    TaskStartParams startParam = new TaskStartParams(taskId);
    startParam.setMessage(message);
    startParam.setDataKind(TaskDataKind.COMPILE_TASK);
    startParam.setData(new CompileTask(this.btId));
    client.onBuildTaskStart(startParam);
  }

  @Override
  public void taskInProgress(String message) {
    TaskProgressParams progressParam = new TaskProgressParams(taskId);
    progressParam.setMessage(message);
    progressParam.setDataKind(TaskDataKind.COMPILE_TASK);
    client.onBuildTaskProgress(progressParam);
  }

  @Override
  public void taskFinished(String message, StatusCode statusCode) {
    TaskFinishParams endParam = new TaskFinishParams(taskId, statusCode);
    endParam.setMessage(message);
    endParam.setDataKind(TaskDataKind.COMPILE_REPORT);
    endParam.setData(new CompileReport(this.btId, 0, 0)); // TODO: parse the errors and warnings
    client.onBuildTaskFinish(endParam);
  }
}

