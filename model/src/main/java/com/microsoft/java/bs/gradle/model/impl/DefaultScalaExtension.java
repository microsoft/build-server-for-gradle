// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import com.microsoft.java.bs.gradle.model.ScalaExtension;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
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
  public Object convert(ClassLoader classLoader) {
    try {
      Class<?> destinationClass = classLoader.loadClass(getClass().getName());
      Object result = destinationClass.getConstructor().newInstance();
      destinationClass.getDeclaredMethod("setScalaCompilerArgs", List.class)
          .invoke(result, getScalaCompilerArgs());
      destinationClass.getDeclaredMethod("setScalaOrganization", String.class)
          .invoke(result, getScalaOrganization());
      destinationClass.getDeclaredMethod("setScalaVersion", String.class)
          .invoke(result, getScalaVersion());
      destinationClass.getDeclaredMethod("setScalaBinaryVersion", String.class)
          .invoke(result, getScalaBinaryVersion());
      destinationClass.getDeclaredMethod("setScalaJars", List.class)
          .invoke(result, getScalaJars());
      return result;
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
              | NoSuchMethodException | ClassNotFoundException | InstantiationException
              | SecurityException e) {
      throw new IllegalStateException("Error converting " + getClass().getName(), e);
    }
  }

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
}
