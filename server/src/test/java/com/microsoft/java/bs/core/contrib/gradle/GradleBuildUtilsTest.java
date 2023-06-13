package com.microsoft.java.bs.core.contrib.gradle;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.model.Preferences;

class GradleBuildUtilsTest {

  @Test
  void testGetProjectConnectionWhenWrapperEnabled() {
    Preferences preferences = mock(Preferences.class);
    when(preferences.getGradleVersion()).thenReturn("1.0-rc-1");

    GradleBuildUtils.getProjectConnection(new File("test"), preferences);
    verify(preferences, times(1)).getGradleUserHome();
    verify(preferences, times(1)).isGradleWrapperEnabled();
    verify(preferences, times(2)).getGradleVersion();
    verify(preferences, never()).getGradleHome();
  }

  @Test
  void testGetBuildLauncher() {
    Preferences preferences = mock(Preferences.class);
    when(preferences.getGradleJavaHome()).thenReturn("test");
    when(preferences.getGradleArguments()).thenReturn(Arrays.asList("test"));
    when(preferences.getGradleJvmArguments()).thenReturn(Arrays.asList("test"));

    GradleBuildUtils.getBuildLauncher(
        GradleBuildUtils.getProjectConnection(new File("test"), preferences),
        preferences
    );
    verify(preferences, times(2)).getGradleJavaHome();
    verify(preferences, times(2)).getGradleArguments();
    verify(preferences, times(2)).getGradleJvmArguments();
  }

  @Test
  void testGetModelBuilder() {
    Preferences preferences = mock(Preferences.class);
    when(preferences.getGradleJavaHome()).thenReturn("test");
    when(preferences.getGradleArguments()).thenReturn(Arrays.asList("test"));
    when(preferences.getGradleJvmArguments()).thenReturn(Arrays.asList("test"));

    GradleBuildUtils.getModelBuilder(
        GradleBuildUtils.getProjectConnection(new File("test"), preferences),
        preferences,
        Map.class
    );
    verify(preferences, times(2)).getGradleJavaHome();
    verify(preferences, times(2)).getGradleArguments();
    verify(preferences, times(2)).getGradleJvmArguments();
  }
}
