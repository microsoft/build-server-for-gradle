package com.microsoft.java.bs.core.internal.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.Constants;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.Preferences;

import ch.epfl.scala.bsp4j.BuildClientCapabilities;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;

class LifecycleServiceTest {

  @Test
  void testInitializeServer() {
    BuildClientCapabilities capabilities = new BuildClientCapabilities(Arrays.asList("java"));
    InitializeBuildParams params = new InitializeBuildParams(
        "test-client",
        "0.1.0",
        "0.1.0",
        Paths.get(System.getProperty("java.io.tmpdir")).toUri().toString(),
        capabilities
    );

    LifecycleService lifecycleService = mock(LifecycleService.class);
    doNothing().when(lifecycleService).updateBuildTargetManager();
    doNothing().when(lifecycleService).initializePreferenceManager(any());
    when(lifecycleService.initializeServer(any())).thenCallRealMethod();

    InitializeBuildResult res = lifecycleService.initializeServer(params);

    assertEquals(Constants.SERVER_NAME, res.getDisplayName());
    assertEquals(Constants.SERVER_VERSION, res.getVersion());
    assertEquals(Constants.BSP_VERSION, res.getBspVersion());
  }

  @Test
  void testInitializePreferenceManager() {
    BuildClientCapabilities capabilities = new BuildClientCapabilities(Arrays.asList("java"));
    InitializeBuildParams params = new InitializeBuildParams(
        "test-client",
        "0.1.0",
        "0.1.0",
        Paths.get(System.getProperty("java.io.tmpdir")).toUri().toString(),
        capabilities
    );
    Preferences preferences = new Preferences();
    preferences.setGradleVersion("17");
    params.setData(preferences);

    PreferenceManager preferenceManager = new PreferenceManager();
    LifecycleService lifecycleService = new LifecycleService(mock(BuildTargetManager.class),
        preferenceManager);
    lifecycleService.initializePreferenceManager(params);

    assertEquals("17", preferenceManager.getPreferences().getGradleVersion());
  }
}
