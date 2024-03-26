// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import java.io.File;
import java.io.Serializable;

/**
 * Represents a source set in a Gradle project.
 */
public interface GradleIncludedBuild extends Serializable {

  /**
   * Equivalent to {@code org.gradle.api.initialization.IncludedBuild.getName()}. 
   */
  public String getName();

  /**
   * Equivalent to {@code org.gradle.api.initialization.IncludedBuild.getProjectDir()}. 
   */
  public File getProjectDir();
}
