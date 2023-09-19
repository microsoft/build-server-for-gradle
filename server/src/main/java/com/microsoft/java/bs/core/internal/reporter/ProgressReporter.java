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
   * @param message the message to be displayed.
   */
  void taskStarted(String message);

  /**
   * Notify the progress of the task.
   *
   * @param message the message to be displayed.
   */
  void taskInProgress(String message);

  /**
   * Notify the client that a task has been finished.
   *
   * @param message the message to be displayed.
   * @param statusCode the status code of the task.
   */
  void taskFinished(String message, StatusCode statusCode);
}
