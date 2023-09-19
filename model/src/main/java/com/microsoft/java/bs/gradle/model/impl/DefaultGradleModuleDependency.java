// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.microsoft.java.bs.gradle.model.GradleModuleDependency;
import com.microsoft.java.bs.gradle.model.Artifact;

/**
 * Default implementation of {@link GradleModuleDependency}.
 */
public class DefaultGradleModuleDependency implements GradleModuleDependency {
  private static final long serialVersionUID = 1L;

  private String group;

  private String module;

  private String version;

  private List<Artifact> artifacts;

  /**
   * Instantiates a new default module dependency.
   *
   * @param group group id.
   * @param module module name.
   * @param version version.
   * @param artifacts list of artifacts.
   */
  public DefaultGradleModuleDependency(String group, String module,
      String version, List<Artifact> artifacts) {
    this.group = group;
    this.module = module;
    this.version = version;
    this.artifacts = artifacts;
  }

  /**
   * Copy constructor.
   */
  public DefaultGradleModuleDependency(GradleModuleDependency moduleDependency) {
    this.group = moduleDependency.getGroup();
    this.module = moduleDependency.getModule();
    this.version = moduleDependency.getVersion();
    this.artifacts = moduleDependency.getArtifacts().stream().map(DefaultArtifact::new)
        .collect(Collectors.toList());
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getModule() {
    return module;
  }

  public void setModule(String module) {
    this.module = module;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<Artifact> getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(List<Artifact> artifacts) {
    this.artifacts = artifacts;
  }

  @Override
  public int hashCode() {
    return Objects.hash(group, module, version, artifacts);
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
    DefaultGradleModuleDependency other = (DefaultGradleModuleDependency) obj;
    return Objects.equals(group, other.group) && Objects.equals(module, other.module)
        && Objects.equals(version, other.version) && Objects.equals(artifacts, other.artifacts);
  }
}
