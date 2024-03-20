// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * The extension model for Scala language.
 */
public interface ScalaExtension extends Serializable {
  /**
   * The list of Scala compiler arguments.
   */
  List<String> getScalaCompilerArgs();

  /**
   * The Scala organization for this source set.
   */
  String getScalaOrganization();

  /**
   * The Scala version to compile this source set.
   */
  String getScalaVersion();

  /**
   * The binary version of scalaVersion.
   * For example, 2.12 if scalaVersion is 2.12.4.
   */
  String getScalaBinaryVersion();

  /**
   * A sequence of Scala jars required for compilation.
   * E.g. scala-library, scala-compiler and scala-reflect.
   */
  List<File> getScalaJars();
}
