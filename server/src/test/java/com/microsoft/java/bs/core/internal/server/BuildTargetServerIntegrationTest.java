// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.server;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.Launcher;
import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.services.BuildTargetService;
import com.microsoft.java.bs.core.internal.services.LifecycleService;

import ch.epfl.scala.bsp4j.BuildClientCapabilities;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

// TODO: Move to a dedicated source set for integration tests
class BuildTargetServerIntegrationTest {

  private GradleBuildServer gradleBuildServer;

  @BeforeAll
  static void beforeClass() {
    String pluginDir = Paths.get(System.getProperty("user.dir"),
        "build", "libs", "plugins").toString();
    System.setProperty(Launcher.PROP_PLUGIN_DIR, pluginDir);
  }

  @BeforeEach
  void setUp() {
    BuildTargetManager buildTargetManager = new BuildTargetManager();
    PreferenceManager preferenceManager = new PreferenceManager();
    GradleApiConnector connector = new GradleApiConnector(preferenceManager);
    LifecycleService lifecycleService = new LifecycleService(buildTargetManager,
        connector, preferenceManager);
    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
        connector, preferenceManager);
    gradleBuildServer = new GradleBuildServer(lifecycleService, buildTargetService);
  }

  @AfterAll
  static void afterClass() {
    System.clearProperty(Launcher.PROP_PLUGIN_DIR);
  }

  @Test
  void testServer() {
    File root = Paths.get(
        System.getProperty("user.dir"),
        "..",
        "testProjects",
        "junit5-jupiter-starter-gradle").toFile();
    
    BuildClientCapabilities capabilities = new BuildClientCapabilities(Arrays.asList("java"));
    InitializeBuildParams params = new InitializeBuildParams(
        "test-client",
        "0.1.0",
        "0.1.0",
        root.toURI().toString(),
        capabilities
    );
    gradleBuildServer.buildInitialize(params).join();
    gradleBuildServer.onBuildInitialized();

    WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
        .join();
    assertEquals(2, buildTargetsResult.getTargets().size());

    List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
        .map(target -> target.getId())
        .collect(Collectors.toList());
    CompileParams compileParams = new CompileParams(btIds);
    CompileResult result = gradleBuildServer.buildTargetCompile(compileParams).join();
    assertEquals(StatusCode.OK, result.getStatusCode());
  }
}
