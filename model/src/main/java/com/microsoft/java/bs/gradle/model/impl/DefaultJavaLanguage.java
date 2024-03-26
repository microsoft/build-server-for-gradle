// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import com.microsoft.java.bs.gradle.model.JavaExtension;
import com.microsoft.java.bs.gradle.model.LanguageExtension;
import com.microsoft.java.bs.gradle.model.SupportedLanguage;

import java.util.Map;

/**
 * Default Java implementation of {@link SupportedLanguage}.
 */
public class DefaultJavaLanguage implements SupportedLanguage<JavaExtension> {
  @Override
  public String getBspName() {
    return "java";
  }

  @Override
  public String getGradleName() {
    return "java";
  }

  @Override
  public JavaExtension getExtension(Map<String, LanguageExtension> extensions) {
    return (JavaExtension) extensions.get(getBspName());
  }
}