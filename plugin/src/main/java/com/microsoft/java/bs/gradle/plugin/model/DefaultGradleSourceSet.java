package com.microsoft.java.bs.gradle.plugin.model;

import java.io.File;
import java.io.Serializable;

import org.gradle.api.Project;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;

/**
 * Default implementation of {@link GradleSourceSet}.
 */
public class DefaultGradleSourceSet implements GradleSourceSet, Serializable {
  private static final long serialVersionUID = 1L;

  private String projectName;

  private String projectPath;

  private File projectDir;

  private File rootDir;

  private String sourceSetName;

  /**
   * Construct a default Gradle source set from a Gradle project.
   */
  public DefaultGradleSourceSet(Project project) {
    this.projectName = project.getName();
    this.projectPath = project.getPath();
    this.projectDir = project.getProjectDir();
    this.rootDir = project.getRootDir();
  }

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getProjectPath() {
    return projectPath;
  }

  public void setProjectPath(String projectPath) {
    this.projectPath = projectPath;
  }

  public File getProjectDir() {
    return projectDir;
  }

  public void setProjectDir(File projectDir) {
    this.projectDir = projectDir;
  }

  public File getRootDir() {
    return rootDir;
  }

  public void setRootDir(File rootDir) {
    this.rootDir = rootDir;
  }

  public String getSourceSetName() {
    return sourceSetName;
  }

  public void setSourceSetName(String sourceSetName) {
    this.sourceSetName = sourceSetName;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((projectName == null) ? 0 : projectName.hashCode());
    result = prime * result + ((projectPath == null) ? 0 : projectPath.hashCode());
    result = prime * result + ((projectDir == null) ? 0 : projectDir.hashCode());
    result = prime * result + ((rootDir == null) ? 0 : rootDir.hashCode());
    result = prime * result + ((sourceSetName == null) ? 0 : sourceSetName.hashCode());
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
    DefaultGradleSourceSet other = (DefaultGradleSourceSet) obj;
    if (projectName == null) {
      if (other.projectName != null) {
        return false;
      }
    } else if (!projectName.equals(other.projectName)) {
      return false;
    }
    if (projectPath == null) {
      if (other.projectPath != null) {
        return false;
      }
    } else if (!projectPath.equals(other.projectPath)) {
      return false;
    }
    if (projectDir == null) {
      if (other.projectDir != null) {
        return false;
      }
    } else if (!projectDir.equals(other.projectDir)) {
      return false;
    }
    if (rootDir == null) {
      if (other.rootDir != null) {
        return false;
      }
    } else if (!rootDir.equals(other.rootDir)) {
      return false;
    }
    if (sourceSetName == null) {
      if (other.sourceSetName != null) {
        return false;
      }
    } else if (!sourceSetName.equals(other.sourceSetName)) {
      return false;
    }
    return true;
  }
}
