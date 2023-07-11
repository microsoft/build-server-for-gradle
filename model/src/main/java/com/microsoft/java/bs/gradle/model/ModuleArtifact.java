package com.microsoft.java.bs.gradle.model;

import java.net.URI;

/**
 * Represents a module artifact.
 */
public interface ModuleArtifact {
  public URI getUri();

  /**
   * Returns the classifier of the artifact.
   */
  public ModuleArtifactClassifier getClassifier();
}
