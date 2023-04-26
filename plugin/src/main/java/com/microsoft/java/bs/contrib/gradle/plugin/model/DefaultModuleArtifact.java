package com.microsoft.java.bs.contrib.gradle.plugin.model;

import java.io.Serializable;
import java.net.URI;

import com.microsoft.java.bs.contrib.gradle.model.ModuleArtifact;

/**
 * Default implementation of {@link ModuleArtifact}.
 */
public class DefaultModuleArtifact implements ModuleArtifact, Serializable {
  private static final long serialVersionUID = 1L;

  private URI uri;

  private String classifier;

  public DefaultModuleArtifact(URI uri, String classifier) {
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
    final int prime = 31;
    int result = 1;
    result = prime * result + ((uri == null) ? 0 : uri.hashCode());
    result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
    return result;
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
    DefaultModuleArtifact other = (DefaultModuleArtifact) obj;
    if (uri == null) {
      if (other.uri != null) {
        return false;
      }
    } else if (!uri.equals(other.uri)) {
      return false;
    }
    if (classifier == null) {
      if (other.classifier != null) {
        return false;
      }
    } else if (!classifier.equals(other.classifier)) {
      return false;
    }
    return true;
  }
}
