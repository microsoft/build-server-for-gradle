// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * The extension model for Java language.
 */
public interface JavaExtension extends Serializable {

  /**
   * JDK home file location.
   */
  public File getJavaHome();

  /**
   * The java version this target is supposed to use.
   */
  public String getJavaVersion();

  /**
   * The source compatibility of the source set.
   */
  public String getSourceCompatibility();

  /**
   * The target compatibility of the source set.
   */
  public String getTargetCompatibility();

  /**
   * The list of compiler arguments.
   */
  public List<String> getCompilerArgs();
}
