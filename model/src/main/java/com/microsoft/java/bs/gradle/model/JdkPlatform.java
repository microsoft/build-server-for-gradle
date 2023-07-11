package com.microsoft.java.bs.gradle.model;

import java.io.File;

/**
 * Represents a JDK platform.
 */
public interface JdkPlatform {
  /**
   * JDK home file location.
   */
  public File getJavaHome();

  /**
   * The major Java version of this JDK.
   */
  public String getJavaVersion();

  /**
   * Equivalent to {@code org.gradle.api.tasks.compile.JavaCompile.getSourceCompatibility()}.
   */
  public String getSourceCompatibility();

  /**
   * Equivalent to {@code org.gradle.api.tasks.compile.JavaCompile.getTargetCompatibility()}.
   */
  public String getTargetCompatibility();
}
