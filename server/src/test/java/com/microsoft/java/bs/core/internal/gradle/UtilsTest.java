// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.internal.model.Preferences;

class UtilsTest {

  private final File projectDir = Paths.get(
      System.getProperty("user.dir"),
      "..",
      "testProjects",
      "gradle-4.3-with-wrapper"
  ).toFile();

  @Test
  void testGetFileFromProperty() {
    System.setProperty("test", "test");
    assertNotNull(Utils.getFileFromEnvOrProperty("test"));
  }

  @Test
  void testGetLatestCompatibleJavaVersion() {
    assertEquals("22", Utils.getLatestCompatibleJavaVersion("8.8"));
    assertEquals("21", Utils.getLatestCompatibleJavaVersion("8.5"));
    assertEquals("20", Utils.getLatestCompatibleJavaVersion("8.3"));
    assertEquals("11", Utils.getLatestCompatibleJavaVersion("5.2.4"));
    assertEquals("", Utils.getLatestCompatibleJavaVersion("1.0"));
  }

  @Test
  void testGetOldestCompatibleJavaVersion() {
    assertEquals("1.8", Utils.getOldestCompatibleJavaVersion());
  }

  @Test
  void testPreferencesPriority_wrapperEnabled() {
    Preferences preferences = mock(Preferences.class);
    when(preferences.isWrapperEnabled()).thenReturn(true);
    assertEquals(GradleBuildKind.WRAPPER, Utils.getEffectiveBuildKind(projectDir, preferences));
  }

  @Test
  void testPreferencesPriority_gradleVersionSet() {
    Preferences preferences = mock(Preferences.class);
    when(preferences.isWrapperEnabled()).thenReturn(false);
    when(preferences.getGradleVersion()).thenReturn("8.1");
    assertEquals(GradleBuildKind.SPECIFIED_VERSION,
        Utils.getEffectiveBuildKind(projectDir, preferences));
  }

  @Test
  void testPreferencesPriority_gradleHomeSet() {
    Preferences preferences = mock(Preferences.class);
    when(preferences.isWrapperEnabled()).thenReturn(false);
    when(preferences.getGradleHome()).thenReturn(new File(System.getProperty("java.io.tmpdir"))
        .getAbsolutePath());
    assertEquals(GradleBuildKind.SPECIFIED_INSTALLATION,
        Utils.getEffectiveBuildKind(projectDir, preferences));
  }
}
