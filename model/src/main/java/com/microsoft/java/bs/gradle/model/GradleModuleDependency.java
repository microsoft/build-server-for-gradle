// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import java.io.Serializable;
import java.util.List;

/**
 * Represents a set of artifact dependency.
 */
public interface GradleModuleDependency extends Serializable {
  public String getGroup();

  public String getModule();

  public String getVersion();

  public List<Artifact> getArtifacts();
}
