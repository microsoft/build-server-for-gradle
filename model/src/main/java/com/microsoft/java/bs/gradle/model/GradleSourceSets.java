package com.microsoft.java.bs.gradle.model;

import java.io.Serializable;
import java.util.List;

/**
 * List of all Gradle source set instances.
 */
public interface GradleSourceSets extends Serializable {
  public List<GradleSourceSet> getGradleSourceSets();
}
