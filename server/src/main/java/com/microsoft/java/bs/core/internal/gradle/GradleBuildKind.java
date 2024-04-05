// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.gradle;

/**
 * The kind of Gradle build.
 */
public enum GradleBuildKind {
  /**
   * From Gradle wrapper.
   */
  WRAPPER,
  /**
   * From user specified Gradle version.
   */
  SPECIFIED_VERSION,
  /**
   * From user specified Gradle home.
   */
  SPECIFIED_INSTALLATION,
  /*
   * From the used TAPI.
   */
  TAPI;
}
