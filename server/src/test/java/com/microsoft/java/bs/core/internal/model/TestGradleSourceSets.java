package com.microsoft.java.bs.core.internal.model;

import java.util.List;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

/**
 * Test implementation of {@link GradleSourceSets}.
 */
public class TestGradleSourceSets implements GradleSourceSets {
  private List<GradleSourceSet> gradleSourceSets;

  public List<GradleSourceSet> getGradleSourceSets() {
    return gradleSourceSets;
  }

  public void setGradleSourceSets(List<GradleSourceSet> gradleSourceSets) {
    this.gradleSourceSets = gradleSourceSets;
  }
}
