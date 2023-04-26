package com.microsoft.java.bs.contrib.gradle.plugin.model;

import java.io.Serializable;
import java.util.List;

import com.microsoft.java.bs.contrib.gradle.model.ModuleArtifact;
import com.microsoft.java.bs.contrib.gradle.model.ModuleDependency;

/**
 * Default implementation of {@link ModuleDependency}.
 */
public class DefaultModuleDependency implements ModuleDependency, Serializable {
  private static final long serialVersionUID = 1L;

  private String organization;

  private String name;

  private String version;

  private List<ModuleArtifact> artifacts;

  /**
   * Instantiates a new default module dependency.
   *
   * @param organization name of the organization.
   * @param name name of the module
   * @param version version of the module
   * @param artifacts list of artifacts
   */
  public DefaultModuleDependency(String organization, String name, String version,
      List<ModuleArtifact> artifacts) {
    this.organization = organization;
    this.name = name;
    this.version = version;
    this.artifacts = artifacts;
  }

  public String getOrganization() {
    return organization;
  }

  public void setOrganization(String organization) {
    this.organization = organization;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<ModuleArtifact> getArtifacts() {
    return artifacts;
  }

  public void setArtifacts(List<ModuleArtifact> artifacts) {
    this.artifacts = artifacts;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((organization == null) ? 0 : organization.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    result = prime * result + ((artifacts == null) ? 0 : artifacts.hashCode());
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
    DefaultModuleDependency other = (DefaultModuleDependency) obj;
    if (organization == null) {
      if (other.organization != null) {
        return false;
      }
    } else if (!organization.equals(other.organization)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    if (version == null) {
      if (other.version != null) {
        return false;
      }
    } else if (!version.equals(other.version)) {
      return false;
    }
    if (artifacts == null) {
      if (other.artifacts != null) {
        return false;
      }
    } else if (!artifacts.equals(other.artifacts)) {
      return false;
    }
    return true;
  }
}
