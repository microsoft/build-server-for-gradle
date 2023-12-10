// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import java.util.UUID;
import java.util.Map;
import java.util.Set;

import com.microsoft.java.bs.core.Launcher;

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

  private final TaskId taskId;
  private final Map<String, Set<BuildTargetIdentifier>> taskPathMap;
  private BuildClient client;

  /**
   * Instantiates a {@link CompileProgressReporter}.
   *
   * @param originId bsp supplied unique id.
   * @param taskPathMap all know task paths to their build targets.
   */
  public CompileProgressReporter(String originId,
      Map<String, Set<BuildTargetIdentifier>> taskPathMap) {
    this.taskId = new TaskId(originId != null ? originId : UUID.randomUUID().toString());
    this.taskPathMap = taskPathMap;
    client = Launcher.client;
  }

  public void cleanUp() {
    client = null;
  }

  @Override
  public void taskStarted(String taskPath, String message, long startTime) {
    BuildClient localClient = client;
    if (localClient != null) {
      Set<BuildTargetIdentifier> targets = taskPathMap.get(taskPath);
      if (targets != null) {
        targets.forEach(btId -> {
          TaskStartParams startParam = new TaskStartParams(taskId);
          startParam.setEventTime(startTime);
          startParam.setMessage(message);
          startParam.setDataKind(TaskDataKind.COMPILE_TASK);
          startParam.setData(new CompileTask(btId));
          localClient.onBuildTaskStart(startParam);
        });
      }
    }
  }

  @Override
  public void taskInProgress(String taskPath, String message, long startTime) {
    BuildClient localClient = client;
    if (localClient != null) {
      Set<BuildTargetIdentifier> targets = taskPathMap.get(taskPath);
      if (targets != null) {
        targets.forEach(btId -> {
          TaskProgressParams progressParam = new TaskProgressParams(taskId);
          progressParam.setEventTime(startTime);
          progressParam.setMessage(message);
          progressParam.setDataKind(TaskDataKind.COMPILE_TASK);
          progressParam.setData(new CompileTask(btId));
          localClient.onBuildTaskProgress(progressParam);
        });
      }
    }
  }

  @Override
  public void taskFinished(String taskPath, String message,
      long startTime, StatusCode statusCode) {
    BuildClient localClient = client;
    if (localClient != null) {
      Set<BuildTargetIdentifier> targets = taskPathMap.get(taskPath);
      if (targets != null) {
        targets.forEach(btId -> {
          TaskFinishParams endParam = new TaskFinishParams(taskId, statusCode);
          endParam.setEventTime(startTime);
          endParam.setMessage(message);
          endParam.setDataKind(TaskDataKind.COMPILE_REPORT);
          endParam.setData(new CompileReport(btId, 0, 0)); // TODO: parse the errors and warnings
          localClient.onBuildTaskFinish(endParam);
        });
      }
    }
  }
}

