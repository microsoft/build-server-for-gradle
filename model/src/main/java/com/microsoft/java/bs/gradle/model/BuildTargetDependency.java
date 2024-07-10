// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import java.io.Serializable;

/**
 * Represents a build target dependency.
 * In Java this is a project:sourceset dependency on a project:sourceset.
 */
public interface BuildTargetDependency extends Serializable {
  public String getProjectDir();

  public String getSourceSetName();
}
