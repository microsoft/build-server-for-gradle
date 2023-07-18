package com.microsoft.java.bs.gradle.model;

import java.util.List;

/**
 * Represents a set of artifact dependency.
 */
public interface ArtifactsDependency {
  public String getGroup();

  public String getModule();

  public String getVersion();

  public List<Artifact> getArtifacts();
}
