// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import com.microsoft.java.bs.gradle.model.LanguageExtension;
import com.microsoft.java.bs.gradle.model.ScalaExtension;
import com.microsoft.java.bs.gradle.model.SupportedLanguage;

import java.util.Map;

/**
 * Default Scala implementation of {@link SupportedLanguage}.
 */
public class DefaultScalaLanguage implements SupportedLanguage<ScalaExtension> {

  @Override
  public String getBspName() {
    return "scala";
  }

  @Override
  public String getGradleName() {
    return "scala";
  }

  @Override
  public ScalaExtension getExtension(Map<String, LanguageExtension> extensions) {
    LanguageExtension extension = extensions.get(getBspName());
    if (extension == null) {
      return null;
    }
    if (extension.isScalaExtension()) {
      return extension.getAsScalaExtension();
    }
    throw new IllegalArgumentException(
        "LanguageExtension: " + extension + " is not a ScalaExtension."
    );
  }
}