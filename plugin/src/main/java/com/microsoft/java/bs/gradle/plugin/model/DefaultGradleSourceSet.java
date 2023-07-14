package com.microsoft.java.bs.gradle.plugin.model;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;
import java.util.Set;

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

  private Set<File> sourceDirs;

  private Set<File> generatedSourceDirs;

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

  public Set<File> getSourceDirs() {
    return sourceDirs;
  }

  public void setSourceDirs(Set<File> sourceDirs) {
    this.sourceDirs = sourceDirs;
  }

  public Set<File> getGeneratedSourceDirs() {
    return generatedSourceDirs;
  }

  public void setGeneratedSourceDirs(Set<File> generatedSourceDirs) {
    this.generatedSourceDirs = generatedSourceDirs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectName, projectPath, projectDir, rootDir,
        sourceSetName, sourceDirs, generatedSourceDirs);
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
    return Objects.equals(projectName, other.projectName)
        && Objects.equals(projectPath, other.projectPath)
        && Objects.equals(projectDir, other.projectDir)
        && Objects.equals(rootDir, other.rootDir)
        && Objects.equals(sourceSetName, other.sourceSetName)
        && Objects.equals(sourceDirs, other.sourceDirs)
        && Objects.equals(generatedSourceDirs, other.generatedSourceDirs);
  }
}
