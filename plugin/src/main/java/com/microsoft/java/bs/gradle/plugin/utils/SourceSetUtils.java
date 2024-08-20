package com.microsoft.java.bs.gradle.plugin.utils;

/**
 * Utility class for common source set operations.
 */
public class SourceSetUtils {

  private SourceSetUtils() {
  }

  /**
   * Returns the gradle project path without the initial {@code :}.
   *
   * @param projectPath project path to operate upon
   */
  public static String stripPathPrefix(String projectPath) {
    if (projectPath.startsWith(":")) {
      return projectPath.substring(1);
    }
    return projectPath;
  }

  /**
   * Return a project task name - [project path]:[task].
   *
   * @param modulePath path of project module
   * @param taskName name of gradle task
   */
  public static String getFullTaskName(String modulePath, String taskName) {
    if (taskName == null) {
      return null;
    }
    if (taskName.isEmpty()) {
      return taskName;
    }

    if (modulePath == null || modulePath.equals(":")) {
      // must be prefixed with ":" as taskPaths are reported back like that in progress messages
      return ":" + taskName;
    }
    return modulePath + ":" + taskName;
  }

}
