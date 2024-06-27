// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import org.gradle.internal.impldep.javax.annotation.Nullable;

import java.io.Serializable;

/**
 * parent interface for language extensions.
 */
public interface LanguageExtension extends Serializable {

  @Nullable
  boolean isJavaExtension();

  @Nullable
  boolean isScalaExtension();

  @Nullable
  JavaExtension getAsJavaExtension();

  @Nullable
  ScalaExtension getAsScalaExtension();

}
