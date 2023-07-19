package com.microsoft.java.bs.gradle.plugin.model;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

import com.microsoft.java.bs.gradle.model.Artifact;

/**
 * Default implementation of {@link Artifact}.
 */
public class DefaultArtifact implements Artifact, Serializable {
  private static final long serialVersionUID = 1L;

  private URI uri;

  private String classifier;

  public DefaultArtifact(URI uri, String classifier) {
    this.uri = uri;
    this.classifier = classifier;
  }

  public URI getUri() {
    return uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public String getClassifier() {
    return classifier;
  }

  public void setClassifier(String classifier) {
    this.classifier = classifier;
  }

  @Override
  public int hashCode() {
    return Objects.hash(uri, classifier);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    DefaultArtifact other = (DefaultArtifact) obj;
    return Objects.equals(uri, other.uri) && Objects.equals(classifier, other.classifier);
  }
}
