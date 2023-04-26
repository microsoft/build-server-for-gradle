package com.microsoft.java.bs.core.contrib;

import ch.epfl.scala.bsp4j.StatusCode;

/**
 * A progress reporter that reports the progress of a task.
 */
public interface ProgressReporter {

    /**
     * Notify the client that a task has been started.
     * @param message
     */
    void taskStarted(String message);

    /**
     * Notify the progress of the task.
     * @param message
     */
    void taskInProgress(String message);

    /**
     * Notify the client that a task has been finished.
     * @param message
     * @param statusCode
     */
    void taskFinished(String message, StatusCode statusCode);
}
