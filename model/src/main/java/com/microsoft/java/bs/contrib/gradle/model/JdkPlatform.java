package com.microsoft.java.bs.contrib.gradle.model;

import java.io.File;

/**
 * Represents a JDK platform.
 */
public interface JdkPlatform {
  public File getJavaHome();

  public String getJavaVersion();

  public String getSourceLanguageLevel();

  public String getTargetBytecodeVersion();
}
