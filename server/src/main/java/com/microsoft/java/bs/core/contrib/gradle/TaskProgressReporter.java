package com.microsoft.java.bs.core.contrib.gradle;

import org.gradle.tooling.events.ProgressEvent;
import org.gradle.tooling.events.ProgressListener;

import com.microsoft.java.bs.core.contrib.ProgressReporter;

import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Reports the progress via {@link ProgressReporter}.
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
