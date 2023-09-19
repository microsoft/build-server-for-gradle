// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import java.io.Serializable;

/**
 * Represents a project dependency.
 */
public interface GradleProjectDependency extends Serializable {
  public String getProjectPath();
}
