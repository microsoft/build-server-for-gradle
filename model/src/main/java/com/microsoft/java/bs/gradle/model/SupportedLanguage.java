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

  E convert(Map<String, LanguageExtension> extensions);
}
