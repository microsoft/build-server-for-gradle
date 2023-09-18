package com.microsoft.java.bs.gradle.model;

import java.io.Serializable;
import java.net.URI;

/**
 * Represents an artifact.
 */
public interface Artifact extends Serializable {
  public URI getUri();

  /**
   * Returns the classifier of the artifact.
   */
  public String getClassifier();
}
