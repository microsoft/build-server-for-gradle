// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.microsoft.java.bs.gradle.model.SupportedLanguages;
import com.microsoft.java.bs.gradle.model.impl.DefaultJavaExtension;
import com.microsoft.java.bs.gradle.model.impl.DefaultScalaExtension;

/**
 * Utils to convert objects between two different class loaders to avoid
 * ClassCastException.
 */
public class Conversions {

  /**
   * copy the extension object to handle using different ClassLoaders.
   */
  public static Object convertExtension(String language, Object extension) {
    if (extension == null) {
      return null;
    }
    if (language.equals(SupportedLanguages.JAVA.getBspName())) {
      return toJavaExtension(extension);
    } else if (language.equals(SupportedLanguages.SCALA.getBspName())) {
      return toScalaExtension(extension);
    } else {
      throw new IllegalStateException("Unsupported language extension conversion "  + language);
    }
  }

  /**
   * Convert an object to a DefaultJavaExtension.
   */
  private static DefaultJavaExtension toJavaExtension(Object object) {
    DefaultJavaExtension result = new DefaultJavaExtension();
    try {
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

  /**
   * Convert an object to a DefaultScalaExtension.
   */
  private static DefaultScalaExtension toScalaExtension(Object object) {
    DefaultScalaExtension result = new DefaultScalaExtension();
    try {
      result.setScalaCompilerArgs(
              (List<String>) object.getClass().getDeclaredMethod("getScalaCompilerArgs")
                      .invoke(object));
      result.setScalaOrganization(
              (String) object.getClass().getDeclaredMethod("getScalaOrganization").invoke(object));
      result.setScalaVersion(
              (String) object.getClass().getDeclaredMethod("getScalaVersion").invoke(object));
      result.setScalaBinaryVersion(
              (String) object.getClass().getDeclaredMethod("getScalaBinaryVersion").invoke(object));
      result.setScalaJars(
              (List<File>) object.getClass().getDeclaredMethod("getScalaJars").invoke(object));
    } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException
             | NoSuchMethodException | SecurityException e) {
      return null;
    }
    return result;
  }
}
