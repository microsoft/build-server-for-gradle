// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import com.microsoft.java.bs.gradle.model.JavaExtension;
import com.microsoft.java.bs.gradle.model.ScalaExtension;

import java.io.File;
import java.util.List;
import java.util.Objects;

/**
 * Default implementation of {@link ScalaExtension}.
 */
public class DefaultScalaExtension implements ScalaExtension {
  private static final long serialVersionUID = 1L;

  private List<String> scalaCompilerArgs;

  private String scalaOrganization;

  private String scalaVersion;

  private String scalaBinaryVersion;

  private List<File> scalaJars;

  @Override
  public List<String> getScalaCompilerArgs() {
    return scalaCompilerArgs;
  }

  public void setScalaCompilerArgs(List<String> scalaCompilerArgs) {
    this.scalaCompilerArgs = scalaCompilerArgs;
  }

  @Override
  public String getScalaOrganization() {
    return scalaOrganization;
  }

  public void setScalaOrganization(String scalaOrganization) {
    this.scalaOrganization = scalaOrganization;
  }

  @Override
  public String getScalaVersion() {
    return scalaVersion;
  }

  public void setScalaVersion(String scalaVersion) {
    this.scalaVersion = scalaVersion;
  }

  @Override
  public String getScalaBinaryVersion() {
    return scalaBinaryVersion;
  }

  public void setScalaBinaryVersion(String scalaBinaryVersion) {
    this.scalaBinaryVersion = scalaBinaryVersion;
  }

  @Override
  public List<File> getScalaJars() {
    return scalaJars;
  }

  public void setScalaJars(List<File> scalaJars) {
    this.scalaJars = scalaJars;
  }

  @Override
  public int hashCode() {
    return Objects.hash(scalaCompilerArgs, scalaOrganization,
      scalaVersion, scalaBinaryVersion, scalaJars);
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
    DefaultScalaExtension other = (DefaultScalaExtension) obj;
    return Objects.equals(scalaCompilerArgs, other.scalaCompilerArgs)
            && Objects.equals(scalaOrganization, other.scalaOrganization)
            && Objects.equals(scalaVersion, other.scalaVersion)
            && Objects.equals(scalaBinaryVersion, other.scalaBinaryVersion)
            && Objects.equals(scalaJars, other.scalaJars);
  }

  @Override
  public boolean isJavaExtension() {
    return false;
  }

  @Override
  public boolean isScalaExtension() {
    return true;
  }

  @Override
  public JavaExtension getAsJavaExtension() {
    return null;
  }

  @Override
  public ScalaExtension getAsScalaExtension() {
    return this;
  }
}
