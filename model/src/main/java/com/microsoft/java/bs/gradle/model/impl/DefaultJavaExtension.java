// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import java.io.File;
import java.util.List;
import java.util.Objects;

import com.microsoft.java.bs.gradle.model.JavaExtension;

/**
 * Default implementation of {@link JavaExtension}.
 */
public class DefaultJavaExtension implements JavaExtension {
  private static final long serialVersionUID = 123345L;

  private File javaHome;

  private String javaVersion;

  private String sourceCompatibility;

  private String targetCompatibility;

  private List<String> compilerArgs;

  @Override
  public File getJavaHome() {
    return javaHome;
  }

  public void setJavaHome(File javaHome) {
    this.javaHome = javaHome;
  }

  @Override
  public String getJavaVersion() {
    return javaVersion;
  }

  public void setJavaVersion(String javaVersion) {
    this.javaVersion = javaVersion;
  }

  @Override
  public String getSourceCompatibility() {
    return sourceCompatibility;
  }

  public void setSourceCompatibility(String sourceCompatibility) {
    this.sourceCompatibility = sourceCompatibility;
  }

  @Override
  public String getTargetCompatibility() {
    return targetCompatibility;
  }

  public void setTargetCompatibility(String targetCompatibility) {
    this.targetCompatibility = targetCompatibility;
  }

  @Override
  public List<String> getCompilerArgs() {
    return compilerArgs;
  }

  public void setCompilerArgs(List<String> compilerArgs) {
    this.compilerArgs = compilerArgs;
  }

  @Override
  public int hashCode() {
    return Objects.hash(javaHome, javaVersion,
        sourceCompatibility, targetCompatibility, compilerArgs
    );
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
    DefaultJavaExtension other = (DefaultJavaExtension) obj;
    return Objects.equals(javaHome, other.javaHome)
        && Objects.equals(javaVersion, other.javaVersion)
        && Objects.equals(sourceCompatibility, other.sourceCompatibility)
        && Objects.equals(targetCompatibility, other.targetCompatibility)
        && Objects.equals(compilerArgs, other.compilerArgs);
  }
}
