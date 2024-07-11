// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import com.microsoft.java.bs.gradle.model.JavaExtension;
import com.microsoft.java.bs.gradle.model.ScalaExtension;

import java.io.File;
import java.util.List;
import java.util.Objects;
import java.util.Set;

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
  
  private Set<File> sourceDirs;

  private Set<File> generatedSourceDirs;

  private String compileTaskName;

  private File classesDir;

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
  public Set<File> getSourceDirs() {
    return sourceDirs;
  }

  public void setSourceDirs(Set<File> sourceDirs) {
    this.sourceDirs = sourceDirs;
  }

  @Override
  public Set<File> getGeneratedSourceDirs() {
    return generatedSourceDirs;
  }

  public void setGeneratedSourceDirs(Set<File> generatedSourceDirs) {
    this.generatedSourceDirs = generatedSourceDirs;
  }

  @Override
  public String getCompileTaskName() {
    return compileTaskName;
  }

  public void setCompileTaskName(String compileTaskName) {
    this.compileTaskName = compileTaskName;
  }

  @Override
  public File getClassesDir() {
    return classesDir;
  }

  public void setClassesDir(File classesDir) {
    this.classesDir = classesDir;
  }

  @Override
  public int hashCode() {
    return Objects.hash(scalaCompilerArgs, scalaOrganization, scalaVersion, scalaBinaryVersion,
        scalaJars, sourceDirs, generatedSourceDirs, compileTaskName, classesDir);
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
            && Objects.equals(scalaJars, other.scalaJars)
            && Objects.equals(sourceDirs, other.sourceDirs)
            && Objects.equals(generatedSourceDirs, other.generatedSourceDirs)
            && Objects.equals(compileTaskName, other.compileTaskName)
            && Objects.equals(classesDir, other.classesDir);
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
