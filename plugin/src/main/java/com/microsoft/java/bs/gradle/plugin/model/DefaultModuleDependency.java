package com.microsoft.java.bs.gradle.plugin.model;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import com.microsoft.java.bs.gradle.model.ModuleDependency;
import com.microsoft.java.bs.gradle.model.Artifact;

/**
 * Default implementation of {@link ModuleDependency}.
 */
public class DefaultModuleDependency implements ModuleDependency, Serializable {
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
  public DefaultModuleDependency(String group, String module,
      String version, List<Artifact> artifacts) {
    this.group = group;
    this.module = module;
    this.version = version;
    this.artifacts = artifacts;
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
    DefaultModuleDependency other = (DefaultModuleDependency) obj;
    return Objects.equals(group, other.group) && Objects.equals(module, other.module)
        && Objects.equals(version, other.version) && Objects.equals(artifacts, other.artifacts);
  }
}
