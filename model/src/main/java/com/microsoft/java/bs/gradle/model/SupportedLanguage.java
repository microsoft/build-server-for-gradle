// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import java.util.Map;

/**
 * The supported language interface.
 */
public interface SupportedLanguage<E extends LanguageExtension> {

  String getBspName();

  String getGradleName();

  /**
   * Returns the correct type for this language extension from language extension map.
   */
  E getExtension(Map<String, LanguageExtension> extensions);

  /**
   * Returns the correct type for this language extension from GradleSourceSet.
   */
  default E getExtension(GradleSourceSet gradleSourceSet) {
    return getExtension(gradleSourceSet.getExtensions());
  }
}
