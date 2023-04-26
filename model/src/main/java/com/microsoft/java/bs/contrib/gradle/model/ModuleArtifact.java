package com.microsoft.java.bs.contrib.gradle.model;

import java.net.URI;

/**
 * Represents a module artifact.
 */
public interface ModuleArtifact {
  public URI getUri();

  public String getClassifier();
}
