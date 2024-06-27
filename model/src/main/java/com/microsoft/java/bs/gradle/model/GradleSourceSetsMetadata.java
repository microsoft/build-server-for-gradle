package com.microsoft.java.bs.gradle.model;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Provides necessary information to build SourceSets.
 */
public interface GradleSourceSetsMetadata extends Serializable {
  Map<GradleSourceSet, List<File>> getGradleSourceSets();

  Map<File, GradleSourceSet> getOutputsToSourceSet();
}
