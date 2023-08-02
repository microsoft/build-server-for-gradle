package com.microsoft.java.bs.core.internal.reporter;

import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;

import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Implements {@link ProgressListener} that listens to the progress of gradle tasks,
 * and reports the progress to the client.
 */
public class TaskProgressReporter implements ProgressListener {

  private ProgressReporter reporter;

  public TaskProgressReporter(ProgressReporter reporter) {
    this.reporter = reporter;
  }

  @Override
  public void statusChanged(ProgressEvent event) {
    taskInProgress(event.getDisplayName());
  }

  public void taskStarted(String message) {
    reporter.taskStarted(message);
  }

  public void taskInProgress(String message) {
    reporter.taskInProgress(message);
  }

  public void taskFinished(String message, StatusCode statusCode) {
    reporter.taskFinished(message, statusCode);
  }
}
