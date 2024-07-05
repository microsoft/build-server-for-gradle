package com.microsoft.java.bs.gradle.model;

import java.io.File;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Provides necessary information about Gradle source sets,
 * enabling mapping of dependencies between them.
 */
public interface GradleSourceSetsMetadata extends Serializable {

  /**
   * Returns a map that associates each Gradle source set with its corresponding
   * classpath files in a gradle project. This typically includes any libraries
   * or dependencies required for compilation within that source set.
   *
   * <p>
   * The keys of the map represent instances of the {@link GradleSourceSet} class,
   * identifying all the source sets within the project.
   * </p>
   * <p>
   * The values of the map are lists of {@link File} objects, representing the
   * classpath files associated with the corresponding source set.
   * </p>
   */
  Map<GradleSourceSet, List<File>> getGradleSourceSetsToClasspath();

  /**
   * Returns a map that associates output files with the Gradle source sets that
   * generated them. This is useful for understanding the origin of generated artifacts.
   *
   * <p>
   * The keys of the map are {@link File} objects, representing individual
   * output files produced during the build process.
   * </p>
   * <p>
   * The values of the map are instances of the {@link GradleSourceSet} class,
   * indicating the source set that generated the corresponding output file.
   * </p>
   */
  Map<File, GradleSourceSet> getOutputsToSourceSet();

}
