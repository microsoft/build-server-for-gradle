// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.impl;

import com.microsoft.java.bs.gradle.model.JavaExtension;
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
  public JavaExtension convert(Map<String, Object> extensions) {
    Object extension = extensions.get(getBspName());
    if (extension == null) {
      return null;
    }
    return (JavaExtension) extension;
  }
}