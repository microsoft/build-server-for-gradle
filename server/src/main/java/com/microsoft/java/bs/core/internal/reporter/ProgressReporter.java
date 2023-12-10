// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.reporter;

import ch.epfl.scala.bsp4j.StatusCode;

/**
 * A progress reporter that reports the progress of a task.
 */
public interface ProgressReporter {

  /**
   * Notify the client that a task has been started.
   *
   * @param taskPath the Gradle task.
   * @param message the message to be displayed.
   * @param startTime when the event started in milliseconds since Epoch.
   */
  void taskStarted(String taskPath, String message, long startTime);

  /**
   * Notify the progress of the task.
   *
   * @param taskPath the Gradle task.
   * @param message the message to be displayed.
   * @param startTime when the event started in milliseconds since Epoch.
   */
  void taskInProgress(String taskPath, String message, long startTime);

  /**
   * Notify the client that a task has been finished.
   *
   * @param taskPath the Gradle task.
   * @param message the message to be displayed.
   * @param startTime when the event started in milliseconds since Epoch.
   * @param statusCode the status code of the task.
   */
  void taskFinished(String taskPath, String message, long startTime,
      StatusCode statusCode);
}
