package com.microsoft.java.bs.core.internal.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.internal.model.Preferences;

class UtilsTest {

  @Test
  void testGetFileFromProperty() {
    System.setProperty("test", "test");
    assertNotNull(Utils.getFileFromEnvOrProperty("test"));
  }

  @Test
  void testGetHighestCompatibleJavaVersion() {
    assertEquals("20", Utils.getHighestCompatibleJavaVersion("8.3"));
    assertEquals("11", Utils.getHighestCompatibleJavaVersion("5.2.4"));
    assertEquals("", Utils.getHighestCompatibleJavaVersion("1.0"));
  }

  @Test
  void testGetGradleVersion() {
    File projectDir = Paths.get(
        System.getProperty("user.dir"),
        "..",
        "testProjects",
        "gradle-4.3-with-wrapper"
    ).normalize().toFile();

    assertEquals("4.3", Utils.getGradleVersion(projectDir.toURI()));
  }

  @Test
  void testPreferencesPriority_wrapperEnabled() {
    Preferences preferences = mock(Preferences.class);
    when(preferences.isWrapperEnabled()).thenReturn(true);
    Utils.getProjectConnection(new File(""), preferences);
    verify(preferences, atLeastOnce()).isWrapperEnabled();
    verify(preferences, never()).getGradleVersion();
    verify(preferences, never()).getGradleHome();
  }

  @Test
  void testPreferencesPriority_gradleVersionSet() {
    Preferences preferences = mock(Preferences.class);
    when(preferences.isWrapperEnabled()).thenReturn(false);
    when(preferences.getGradleVersion()).thenReturn("8.1");
    Utils.getProjectConnection(new File(""), preferences);
    verify(preferences, atLeastOnce()).isWrapperEnabled();
    verify(preferences, atLeastOnce()).getGradleVersion();
    verify(preferences, never()).getGradleHome();
  }

  @Test
  void testPreferencesPriority_gradleHomeSet() {
    Preferences preferences = mock(Preferences.class);
    when(preferences.isWrapperEnabled()).thenReturn(false);
    when(preferences.getGradleVersion()).thenReturn("");
    when(preferences.getGradleHome()).thenReturn("test");
    Utils.getProjectConnection(new File(""), preferences);
    verify(preferences, atLeastOnce()).isWrapperEnabled();
    verify(preferences, atLeastOnce()).getGradleVersion();
    verify(preferences, atLeastOnce()).getGradleHome();
  }
}
