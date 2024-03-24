// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import java.io.Serializable;

/**
 * parent interface for language extensions.
 */
public interface LanguageExtension extends Serializable {

  /**
   * clones a language extension.
   * needed for conversion across classloaders.
   * must return Object.
   */
  Object convert(ClassLoader classLoader);
}
