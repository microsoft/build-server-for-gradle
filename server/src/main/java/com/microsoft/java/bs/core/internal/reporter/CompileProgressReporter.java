// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

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

import org.gradle.tooling.events.FailureResult;
import org.gradle.tooling.events.FinishEvent;
import org.gradle.tooling.events.OperationResult;
import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;
import org.gradle.tooling.events.StartEvent;
import org.gradle.tooling.events.task.TaskSkippedResult;
import org.gradle.tooling.events.task.TaskSuccessResult;

/**
 * An implementation of {@link ProgressReporter}
 * and {@link ProgressListener} used for compilation tasks.
 */
public class CompileProgressReporter extends ProgressReporter {

  private final Map<String, Set<BuildTargetIdentifier>> taskPathMap;
  private final Map<String, Long> startTimes;

  /**
   * Instantiates a {@link CompileProgressReporter}.
   *
   * @param client BSP client to report to.
   * @param originId id of the BSP client message.
   * @param taskPathMap all know task paths to their build targets.
   */
  public CompileProgressReporter(BuildClient client, String originId,
      Map<String, Set<BuildTargetIdentifier>> taskPathMap) {
    super(client, originId);
    this.taskPathMap = taskPathMap;
    startTimes = new HashMap<>();
  }

  @Override
  public void statusChanged(ProgressEvent event) {
    if (client != null) {
      String taskPath = getTaskPath(event.getDescriptor());
      TaskId taskId = getTaskId(taskPath);
      Set<BuildTargetIdentifier> targets = taskPathMap.get(taskPath);
      if (targets != null) {
        if (event instanceof StartEvent) {
          startTimes.put(taskPath, event.getEventTime());
          taskStarted(taskId, targets, event.getDisplayName());
        } else if (event instanceof FinishEvent) {
          OperationResult result = ((FinishEvent) event).getResult();
          StatusCode status = result instanceof FailureResult ? StatusCode.ERROR : StatusCode.OK;
          Long compileStartTime = startTimes.get(taskPath);
          Long compileTimeDuration = compileStartTime == null ? null
              : event.getEventTime() - compileStartTime;
          boolean skipped = result instanceof TaskSkippedResult;
          boolean upToDate = result instanceof TaskSuccessResult
              && ((TaskSuccessResult) result).isUpToDate();
          taskFinished(taskId, targets, event.getDisplayName(), compileTimeDuration, status,
              skipped || upToDate);
        } else {
          taskInProgress(taskId, targets, event.getDisplayName());
        }
      }
    }
  }

  private void taskStarted(TaskId taskId, Set<BuildTargetIdentifier> targets, String message) {
    long eventTime = System.currentTimeMillis();
    targets.forEach(btId -> {
      TaskStartParams startParam = new TaskStartParams(taskId);
      startParam.setEventTime(eventTime);
      startParam.setMessage(message);
      startParam.setDataKind(TaskDataKind.COMPILE_TASK);
      startParam.setData(new CompileTask(btId));
      client.onBuildTaskStart(startParam);
    });
  }

  private void taskInProgress(TaskId taskId, Set<BuildTargetIdentifier> targets, String message) {
    long eventTime = System.currentTimeMillis();
    targets.forEach(btId -> {
      TaskProgressParams progressParam = new TaskProgressParams(taskId);
      progressParam.setEventTime(eventTime);
      progressParam.setMessage(message);
      progressParam.setDataKind(TaskDataKind.COMPILE_TASK);
      progressParam.setData(new CompileTask(btId));
      client.onBuildTaskProgress(progressParam);
    });
  }

  private void taskFinished(TaskId taskId, Set<BuildTargetIdentifier> targets, String message,
      Long compileTimeDuration, StatusCode statusCode, boolean noOp) {
    long eventTime = System.currentTimeMillis();
    targets.forEach(btId -> {
      TaskFinishParams endParam = new TaskFinishParams(taskId, statusCode);
      endParam.setEventTime(eventTime);
      endParam.setMessage(message);
      endParam.setDataKind(TaskDataKind.COMPILE_REPORT);
      // TODO Gradle > 8.8 Problems API will allow errors/warnings to be reported on
      CompileReport compileReport = new CompileReport(btId, 0, 0);
      compileReport.setNoOp(noOp);
      compileReport.setOriginId(originId);
      compileReport.setTime(compileTimeDuration);
      endParam.setData(compileReport);
      client.onBuildTaskFinish(endParam);
    });
  }
}

