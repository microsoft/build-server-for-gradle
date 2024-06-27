package com.microsoft.java.bs.gradle.model.impl;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSetsMetadata;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of {@link DefaultGradleSourceSetsMetadata}.
 */
public class DefaultGradleSourceSetsMetadata implements GradleSourceSetsMetadata {

  private Map<GradleSourceSet, List<File>> gradleSourceSets;
  private Map<File, GradleSourceSet> outputsToSourceSet;

  public DefaultGradleSourceSetsMetadata(
      Map<GradleSourceSet, List<File>> gradleSourceSets,
      Map<File, GradleSourceSet> outputsToSourceSet
  ) {
    this.gradleSourceSets = gradleSourceSets;
    this.outputsToSourceSet = outputsToSourceSet;
  }

  // TODO: Copy constructor

  public DefaultGradleSourceSetsMetadata(GradleSourceSetsMetadata gradleSourceSetsMetadata) {
    this(gradleSourceSetsMetadata.getGradleSourceSets(),
        gradleSourceSetsMetadata.getOutputsToSourceSet());
  }

  @Override
  public Map<GradleSourceSet, List<File>> getGradleSourceSets() {
    return gradleSourceSets;
  }

  public void setGradleSourceSets(Map<GradleSourceSet, List<File>> gradleSourceSets) {
    this.gradleSourceSets = gradleSourceSets;
  }

  @Override
  public Map<File, GradleSourceSet> getOutputsToSourceSet() {
    return outputsToSourceSet;
  }

  public void setOutputsToSourceSet(Map<File, GradleSourceSet> outputsToSourceSet) {
    this.outputsToSourceSet = outputsToSourceSet;
  }

  @Override
  public int hashCode() {
    return Objects.hash(gradleSourceSets, outputsToSourceSet);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    DefaultGradleSourceSetsMetadata that = (DefaultGradleSourceSetsMetadata) obj;
    return Objects.equals(gradleSourceSets, that.gradleSourceSets)
        && Objects.equals(outputsToSourceSet, that.outputsToSourceSet);
  }
}
