package com.microsoft.java.bs.gradle.plugin.model;

import java.io.Serializable;
import java.util.Objects;

import com.microsoft.java.bs.gradle.model.GradleProjectDependency;

/**
 * Default implementation of {@link GradleProjectDependency}.
 */
public class DefaultGradleProjectDependency implements GradleProjectDependency, Serializable {
  private static final long serialVersionUID = 1L;

  private String projectPath;

  public DefaultGradleProjectDependency(String projectPath) {
    this.projectPath = projectPath;
  }

  public String getProjectPath() {
    return projectPath;
  }

  public void setProjectPath(String projectPath) {
    this.projectPath = projectPath;
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectPath);
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
    DefaultGradleProjectDependency other = (DefaultGradleProjectDependency) obj;
    return Objects.equals(projectPath, other.projectPath);
  }
}
