// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.microsoft.java.bs.gradle.model.impl.DefaultJavaExtension;

/**
 * Utils to convert objects between two different class loaders to avoid
 * ClassCastException.
 */
public class ConversionUtils {

  /**
   * Convert an object to a DefaultJavaExtension.
   */
  public static DefaultJavaExtension toJavaExtension(Object object) {
    if (object == null) {
      return null;
    }

    DefaultJavaExtension result = new DefaultJavaExtension();
    try {
      result.setCompileClasspath(
          (List<File>) object.getClass().getDeclaredMethod("getCompileClasspath").invoke(object));
      result.setJavaHome((File) object.getClass().getDeclaredMethod("getJavaHome").invoke(object));
      result.setJavaVersion(
          (String) object.getClass().getDeclaredMethod("getJavaVersion").invoke(object));
      result.setSourceCompatibility(
          (String) object.getClass().getDeclaredMethod("getSourceCompatibility").invoke(object));
      result.setTargetCompatibility(
          (String) object.getClass().getDeclaredMethod("getTargetCompatibility").invoke(object));
      result.setCompilerArgs(
          (List<String>) object.getClass().getDeclaredMethod("getCompilerArgs").invoke(object));
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
        | NoSuchMethodException | SecurityException e) {
      return null;
    }
    return result;
  }
}
