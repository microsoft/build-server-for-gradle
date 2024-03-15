// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core;

import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.log.LogHandler;
import com.microsoft.java.bs.core.internal.log.TelemetryHandler;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.server.GradleBuildServer;
import com.microsoft.java.bs.core.internal.services.BuildTargetService;
import com.microsoft.java.bs.core.internal.services.LifecycleService;

import ch.epfl.scala.bsp4j.BuildClient;

/**
 * Main entry point for the BSP server.
 */
public class Launcher {

  public static final Logger LOGGER = Logger.getLogger("GradleBuildServerLogger");

  /**
   * The property name for the directory location storing the plugin and init script.
   */
  public static final String PROP_PLUGIN_DIR = "plugin.dir";

  /**
   * Main entry point.
   */
  public static void main(String[] args) {
    checkRequiredProperties();

    org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> launcher = createLauncher();
    setupLoggers(launcher.getRemoteProxy());
    launcher.startListening();
  }

  private static org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> createLauncher() {
    BuildTargetManager buildTargetManager = new BuildTargetManager();
    PreferenceManager preferenceManager = new PreferenceManager();
    GradleApiConnector connector = new GradleApiConnector(preferenceManager);
    LifecycleService lifecycleService = new LifecycleService(buildTargetManager,
        connector, preferenceManager);
    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
        connector, preferenceManager);
    GradleBuildServer gradleBuildServer = new GradleBuildServer(lifecycleService,
        buildTargetService);
    org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> launcher =
        new org.eclipse.lsp4j.jsonrpc.Launcher.Builder<BuildClient>()
          .setOutput(System.out)
          .setInput(System.in)
          .setLocalService(gradleBuildServer)
          .setRemoteInterface(BuildClient.class)
          .setExecutorService(Executors.newCachedThreadPool())
          .create();
    buildTargetService.setClient(launcher.getRemoteProxy());
    return launcher;
  }

  private static void checkRequiredProperties() {
    if (System.getProperty(PROP_PLUGIN_DIR) == null) {
      throw new IllegalStateException("The property '" + PROP_PLUGIN_DIR + "' is not set");
    }
  }

  private static void setupLoggers(BuildClient client) {
    LOGGER.setUseParentHandlers(false);
    LogHandler logHandler = new LogHandler(client);
    logHandler.setLevel(Level.FINE);
    LOGGER.addHandler(logHandler);

    if (System.getProperty("disableServerTelemetry") == null) {
      TelemetryHandler telemetryHandler = new TelemetryHandler(client);
      telemetryHandler.setLevel(Level.INFO);
      LOGGER.addHandler(telemetryHandler);
    }
  }
}
