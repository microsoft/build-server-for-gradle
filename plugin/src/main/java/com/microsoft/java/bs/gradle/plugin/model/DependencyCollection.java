package com.microsoft.java.bs.gradle.plugin.model;

import java.util.Set;

import com.microsoft.java.bs.gradle.model.ArtifactsDependency;

/**
 * Collection of dependencies.
 */
public class DependencyCollection {
  private Set<ArtifactsDependency> artifactsDependencies;

  /**
   * Instantiates a new dependency collection.
   */
  public DependencyCollection(Set<ArtifactsDependency> artifactsDependencies) {
    this.artifactsDependencies = artifactsDependencies;
  }

  public Set<ArtifactsDependency> getArtifactsDependencies() {
    return artifactsDependencies;
  }

  // TODO: add project dependencies

}
