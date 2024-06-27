// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Default implementation of {@link GradleSourceSets}.
 */
public class DefaultGradleSourceSets implements GradleSourceSets {
  private static final long serialVersionUID = 1L;

  private List<GradleSourceSet> gradleSourceSets;

  public DefaultGradleSourceSets(List<GradleSourceSet> gradleSourceSets) {
    this.gradleSourceSets = gradleSourceSets;
  }

  /**
   * Copy constructor.
   */
  public DefaultGradleSourceSets(GradleSourceSets sourceSets) {
    this(sourceSets.getGradleSourceSets().stream()
        .map(DefaultGradleSourceSet::new)
        .collect(Collectors.toList()));
  }

  @Override
  public List<GradleSourceSet> getGradleSourceSets() {
    return gradleSourceSets;
  }

  public void setGradleSourceSets(List<GradleSourceSet> gradleSourceSets) {
    this.gradleSourceSets = gradleSourceSets;
  }

  @Override
  public int hashCode() {
    return Objects.hash(gradleSourceSets);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DefaultGradleSourceSets other = (DefaultGradleSourceSets) obj;
    return Objects.equals(gradleSourceSets, other.gradleSourceSets);
  }
}
