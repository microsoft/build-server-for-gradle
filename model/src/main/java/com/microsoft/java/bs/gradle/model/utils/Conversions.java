// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model.utils;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.microsoft.java.bs.gradle.model.impl.DefaultJavaExtension;
import com.microsoft.java.bs.gradle.model.impl.DefaultScalaExtension;

/**
 * Utils to convert objects between two different class loaders to avoid
 * ClassCastException.
 */
public class Conversions {

  /**
   * Convert an object to a DefaultJavaExtension.
   */
  public static DefaultJavaExtension toJavaExtension(Object object) {
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
  public static DefaultScalaExtension toScalaExtension(Object object) {
    if (object == null) {
      return null;
    }
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
