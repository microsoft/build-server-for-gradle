// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import com.microsoft.java.bs.gradle.model.impl.DefaultJavaLanguage;
import com.microsoft.java.bs.gradle.model.impl.DefaultScalaLanguage;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The supported languages.
 */
public class SupportedLanguages {
  public static final DefaultJavaLanguage JAVA = new DefaultJavaLanguage();
  public static final DefaultScalaLanguage SCALA = new DefaultScalaLanguage();

  public static final List<SupportedLanguage<?>> all;
  public static final List<String> allBspNames;

  static {
    all = new LinkedList<>();
    all.add(JAVA);
    all.add(SCALA);
    allBspNames = all.stream().map(SupportedLanguage::getBspName).collect(Collectors.toList());
  }
}
