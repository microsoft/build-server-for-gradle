// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSet;

/**
 * The cache for SourceSet (from Gradle) and the customized
 * DefaultGradleSourceSet/Project (from Gradle) mapping.
 */
public class SourceSetCache {
  private Map<SourceSet, DefaultGradleSourceSet> gradleSourceSetMapping;
  private Map<SourceSet, Project> projectMapping;

  public SourceSetCache() {
    this.gradleSourceSetMapping = new HashMap<>();
    this.projectMapping = new HashMap<>();
  }

  public void addGradleSourceSet(SourceSet sourceSet, DefaultGradleSourceSet gradleSourceSet) {
    this.gradleSourceSetMapping.put(sourceSet, gradleSourceSet);
  }

  public void addProject(SourceSet sourceSet, Project project) {
    this.projectMapping.put(sourceSet, project);
  }

  public DefaultGradleSourceSet getGradleSourceSet(SourceSet sourceSet) {
    return this.gradleSourceSetMapping.get(sourceSet);
  }

  public Project getProject(SourceSet sourceSet) {
    return this.projectMapping.get(sourceSet);
  }

  public Collection<DefaultGradleSourceSet> getAllGradleSourceSets() {
    return this.gradleSourceSetMapping.values();
  }

  public Collection<SourceSet> getAllSourceSets() {
    return this.gradleSourceSetMapping.keySet();
  }

  public Collection<Project> getAllProjects() {
    return this.projectMapping.values();
  }
}
