package com.microsoft.java.bs.contrib.gradle.plugin.model;

import java.io.File;
import java.io.Serializable;
import java.util.Set;

import com.microsoft.java.bs.contrib.gradle.model.JdkPlatform;
import com.microsoft.java.bs.contrib.gradle.model.ModuleDependency;
import com.microsoft.java.bs.contrib.gradle.model.ProjectDependency;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTarget;

/**
 * Default implementation of {@link JavaBuildTarget}.
 */
public class DefaultJavaBuildTarget implements JavaBuildTarget, Serializable {
  private static final long serialVersionUID = 1L;

  private String projectName;

  private String modulePath;

  private File projectDir;

  private File rootDir;

  private Set<File> sourceDirs;

  private File sourceOutputDir;

  private Set<File> resourceDirs;

  private File resourceOutputDirs;

  private File apGeneratedDir;

  private Set<File> generatedSourceDirs;

  private Set<ModuleDependency> moduleDependencies;

  private Set<ProjectDependency> projectDependencies;

  private JdkPlatform jdkPlatform;

  private String sourceSetName;

  public String getProjectName() {
    return projectName;
  }

  public void setProjectName(String projectName) {
    this.projectName = projectName;
  }

  public String getModulePath() {
    return modulePath;
  }

  public void setModulePath(String modulePath) {
    this.modulePath = modulePath;
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

  public Set<File> getSourceDirs() {
    return sourceDirs;
  }

  public void setSourceDirs(Set<File> srcDirs) {
    this.sourceDirs = srcDirs;
  }

  public File getSourceOutputDir() {
    return sourceOutputDir;
  }

  public void setSourceOutputDir(File sourceOutputDir) {
    this.sourceOutputDir = sourceOutputDir;
  }

  public Set<File> getResourceDirs() {
    return resourceDirs;
  }

  public void setResourceDirs(Set<File> resourceDirs) {
    this.resourceDirs = resourceDirs;
  }

  public File getResourceOutputDirs() {
    return resourceOutputDirs;
  }

  public void setResourceOutputDirs(File resourceOutputDirs) {
    this.resourceOutputDirs = resourceOutputDirs;
  }

  public File getApGeneratedDir() {
    return apGeneratedDir;
  }

  public void setApGeneratedDir(File apGeneratedDir) {
    this.apGeneratedDir = apGeneratedDir;
  }

  public Set<File> getGeneratedSourceDirs() {
    return generatedSourceDirs;
  }

  public void setGeneratedSourceDirs(Set<File> generatedSourceDirs) {
    this.generatedSourceDirs = generatedSourceDirs;
  }

  public Set<ModuleDependency> getModuleDependencies() {
    return moduleDependencies;
  }

  public void setModuleDependencies(Set<ModuleDependency> dependencyModules) {
    this.moduleDependencies = dependencyModules;
  }

  public Set<ProjectDependency> getProjectDependencies() {
    return projectDependencies;
  }

  public void setProjectDependencies(Set<ProjectDependency> projectDependencies) {
    this.projectDependencies = projectDependencies;
  }

  public JdkPlatform getJdkPlatform() {
    return jdkPlatform;
  }

  public void setJdkPlatform(JdkPlatform jdkPlatform) {
    this.jdkPlatform = jdkPlatform;
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
    result = prime * result + ((modulePath == null) ? 0 : modulePath.hashCode());
    result = prime * result + ((projectDir == null) ? 0 : projectDir.hashCode());
    result = prime * result + ((rootDir == null) ? 0 : rootDir.hashCode());
    result = prime * result + ((sourceDirs == null) ? 0 : sourceDirs.hashCode());
    result = prime * result + ((sourceOutputDir == null) ? 0 : sourceOutputDir.hashCode());
    result = prime * result + ((resourceDirs == null) ? 0 : resourceDirs.hashCode());
    result = prime * result + ((resourceOutputDirs == null) ? 0 : resourceOutputDirs.hashCode());
    result = prime * result + ((apGeneratedDir == null) ? 0 : apGeneratedDir.hashCode());
    result = prime * result + ((generatedSourceDirs == null) ? 0 : generatedSourceDirs.hashCode());
    result = prime * result + ((moduleDependencies == null) ? 0 : moduleDependencies.hashCode());
    result = prime * result + ((projectDependencies == null) ? 0 : projectDependencies.hashCode());
    result = prime * result + ((jdkPlatform == null) ? 0 : jdkPlatform.hashCode());
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
    DefaultJavaBuildTarget other = (DefaultJavaBuildTarget) obj;
    if (projectName == null) {
      if (other.projectName != null) {
        return false;
      }
    } else if (!projectName.equals(other.projectName)) {
      return false;
    }
    if (modulePath == null) {
      if (other.modulePath != null) {
        return false;
      }
    } else if (!modulePath.equals(other.modulePath)) {
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
    if (sourceDirs == null) {
      if (other.sourceDirs != null) {
        return false;
      }
    } else if (!sourceDirs.equals(other.sourceDirs)) {
      return false;
    }
    if (sourceOutputDir == null) {
      if (other.sourceOutputDir != null) {
        return false;
      }
    } else if (!sourceOutputDir.equals(other.sourceOutputDir)) {
      return false;
    }
    if (resourceDirs == null) {
      if (other.resourceDirs != null) {
        return false;
      }
    } else if (!resourceDirs.equals(other.resourceDirs)) {
      return false;
    }
    if (resourceOutputDirs == null) {
      if (other.resourceOutputDirs != null) {
        return false;
      }
    } else if (!resourceOutputDirs.equals(other.resourceOutputDirs)) {
      return false;
    }
    if (apGeneratedDir == null) {
      if (other.apGeneratedDir != null) {
        return false;
      }
    } else if (!apGeneratedDir.equals(other.apGeneratedDir)) {
      return false;
    }
    if (generatedSourceDirs == null) {
      if (other.generatedSourceDirs != null) {
        return false;
      }
    } else if (!generatedSourceDirs.equals(other.generatedSourceDirs)) {
      return false;
    }
    if (moduleDependencies == null) {
      if (other.moduleDependencies != null) {
        return false;
      }
    } else if (!moduleDependencies.equals(other.moduleDependencies)) {
      return false;
    }
    if (projectDependencies == null) {
      if (other.projectDependencies != null) {
        return false;
      }
    } else if (!projectDependencies.equals(other.projectDependencies)) {
      return false;
    }
    if (jdkPlatform == null) {
      if (other.jdkPlatform != null) {
        return false;
      }
    } else if (!jdkPlatform.equals(other.jdkPlatform)) {
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
