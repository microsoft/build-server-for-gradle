// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.model;

import java.io.Serializable;

/**
 * Interface representing a language extension.
 *
 * @see JavaExtension
 * @see ScalaExtension
 */
public interface LanguageExtension extends Serializable {

  /**
   * Checks if the implementing class is a {@link JavaExtension}.
   *
   * @return true if the extension is for Java, false otherwise.
   */
  boolean isJavaExtension();

  /**
   * Checks if the implementing class is a {@link ScalaExtension}.
   *
   * @return true if the extension is for Scala, false otherwise.
   */
  boolean isScalaExtension();

  /**
   * Attempts to cast the current object to a {@link JavaExtension} instance.
   * <p>
   * This method should ideally be used only when the implementing class
   * is known to be a {@link JavaExtension}.
   * </p>
   *
   * @return the current object cast to a {@link JavaExtension} instance,
   *        or null if the cast fails.
   */
  JavaExtension getAsJavaExtension();

  /**
   * Attempts to cast the current object to a {@link ScalaExtension} instance.
   * <p>
   * This method should ideally be used only when the implementing class
   * is known to be a {@link ScalaExtension}.
   * </p>
   *
   * @return the current object cast to a {@link ScalaExtension} instance,
   *        or null if the cast fails.
   */
  ScalaExtension getAsScalaExtension();

}
