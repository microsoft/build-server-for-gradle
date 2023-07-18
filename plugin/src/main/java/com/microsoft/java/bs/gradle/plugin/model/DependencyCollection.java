package com.microsoft.java.bs.gradle.plugin.model;

import java.util.Set;

import com.microsoft.java.bs.gradle.model.ModuleDependency;

/**
 * Collection of dependencies.
 */
public class DependencyCollection {
  private Set<ModuleDependency> moduleDependencies;

  /**
   * Instantiates a new dependency collection.
   */
  public DependencyCollection(Set<ModuleDependency> moduleDependencies) {
    this.moduleDependencies = moduleDependencies;
  }

  public Set<ModuleDependency> getModuleDependencies() {
    return moduleDependencies;
  }

  // TODO: add project dependencies

}
