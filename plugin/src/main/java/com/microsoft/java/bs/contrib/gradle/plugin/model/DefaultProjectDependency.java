package com.microsoft.java.bs.contrib.gradle.plugin.model;

import java.io.Serializable;

import com.microsoft.java.bs.contrib.gradle.model.ProjectDependency;

/**
 * Default implementation of {@link ProjectDependency}.
 */
public class DefaultProjectDependency implements ProjectDependency, Serializable {
  private static final long serialVersionUID = 1L;

  private String projectName;

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public DefaultProjectDependency(String projectName) {
    this.projectName = projectName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((projectName == null) ? 0 : projectName.hashCode());
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
    DefaultProjectDependency other = (DefaultProjectDependency) obj;
    if (projectName == null) {
      if (other.projectName != null) {
        return false;
      }
    } else if (!projectName.equals(other.projectName)) {
      return false;
    }
    return true;
  }

}
