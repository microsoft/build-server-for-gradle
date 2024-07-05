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

  private Map<GradleSourceSet, List<File>> sourceSetsToClasspath;
  private Map<File, GradleSourceSet> outputsToSourceSet;

  public DefaultGradleSourceSetsMetadata(
      Map<GradleSourceSet, List<File>> sourceSetsToClasspath,
      Map<File, GradleSourceSet> outputsToSourceSet
  ) {
    this.sourceSetsToClasspath = sourceSetsToClasspath;
    this.outputsToSourceSet = outputsToSourceSet;
  }

  @Override
  public Map<GradleSourceSet, List<File>> getGradleSourceSetsToClasspath() {
    return sourceSetsToClasspath;
  }

  public void setSourceSetsToClasspath(Map<GradleSourceSet, List<File>> sourceSetsToClasspath) {
    this.sourceSetsToClasspath = sourceSetsToClasspath;
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
    return Objects.hash(sourceSetsToClasspath, outputsToSourceSet);
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
    return Objects.equals(sourceSetsToClasspath, that.sourceSetsToClasspath)
        && Objects.equals(outputsToSourceSet, that.outputsToSourceSet);
  }
}
