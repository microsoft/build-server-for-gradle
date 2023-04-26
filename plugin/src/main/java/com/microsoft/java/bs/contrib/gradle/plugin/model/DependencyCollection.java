package com.microsoft.java.bs.contrib.gradle.plugin.model;

import java.util.Set;

import com.microsoft.java.bs.contrib.gradle.model.ModuleDependency;
import com.microsoft.java.bs.contrib.gradle.model.ProjectDependency;

/**
 * Collection of dependencies.
 */
public class DependencyCollection {
  private Set<ModuleDependency> moduleDependencies;

  private Set<ProjectDependency> projectDependencies;

  /**
   * Instantiates a new dependency collection.
   *
   * @param moduleDependencies module dependencies.
   * @param projectDependencies project dependencies.
   */
  public DependencyCollection(Set<ModuleDependency> moduleDependencies,
      Set<ProjectDependency> projectDependencies) {
    this.moduleDependencies = moduleDependencies;
    this.projectDependencies = projectDependencies;
  }

  public Set<ModuleDependency> getModuleDependencies() {
    return moduleDependencies;
  }

  public Set<ProjectDependency> getProjectDependencies() {
    return projectDependencies;
  }
}
