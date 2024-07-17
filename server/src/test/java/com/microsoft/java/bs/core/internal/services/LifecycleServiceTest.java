// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.Constants;
import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.Preferences;
import com.microsoft.java.bs.gradle.model.SupportedLanguages;

import ch.epfl.scala.bsp4j.BuildClientCapabilities;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.InitializeBuildResult;

class LifecycleServiceTest {

  @Test
  void testInitializeServer() {
    BuildClientCapabilities capabilities =
        new BuildClientCapabilities(SupportedLanguages.allBspNames);
    InitializeBuildParams params = new InitializeBuildParams(
        "test-client",
        "0.1.0",
        "0.1.0",
        Paths.get(System.getProperty("java.io.tmpdir")).toUri().toString(),
        capabilities
    );

    LifecycleService lifecycleService = mock(LifecycleService.class);
    doNothing().when(lifecycleService).initializePreferenceManager(any());
    when(lifecycleService.initializeServer(any())).thenCallRealMethod();

    InitializeBuildResult res = lifecycleService.initializeServer(params);

    assertEquals(Constants.SERVER_NAME, res.getDisplayName());
    assertEquals(Constants.SERVER_VERSION, res.getVersion());
    assertEquals(Constants.BSP_VERSION, res.getBspVersion());
  }

  @Test
  void testInitializePreferenceManager() {
    BuildClientCapabilities capabilities =
        new BuildClientCapabilities(SupportedLanguages.allBspNames);
    InitializeBuildParams params = new InitializeBuildParams(
        "test-client",
        "0.1.0",
        "0.1.0",
        Paths.get(System.getProperty("java.io.tmpdir")).toUri().toString(),
        capabilities
    );
    Preferences preferences = new Preferences();
    preferences.setGradleVersion("8.1");
    params.setData(preferences);

    PreferenceManager preferenceManager = new PreferenceManager();
    LifecycleService lifecycleService = new LifecycleService(
        mock(GradleApiConnector.class), preferenceManager);
    lifecycleService.initializePreferenceManager(params);

    assertEquals("8.1", preferenceManager.getPreferences().getGradleVersion());
  }

  @Test
  void testGetLatestCompatibleJdk() throws URISyntaxException {
    Map<String, String> jdks = new HashMap<>();
    jdks.put("1.8", "file:///path/to/jdk8");
    jdks.put("11", "file:///path/to/jdk11");
    jdks.put("17", "file:///path/to/jdk17");

    assertEquals(new File(new URI("file:///path/to/jdk11")),
        LifecycleService.getLatestCompatibleJdk(jdks, "1.8", "13"));
    assertEquals(new File(new URI("file:///path/to/jdk8")),
        LifecycleService.getLatestCompatibleJdk(jdks, "1.8", "9"));
  }
}
