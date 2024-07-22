// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.log.LogHandler;
import com.microsoft.java.bs.core.internal.log.TelemetryHandler;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.server.GradleBuildServer;
import com.microsoft.java.bs.core.internal.services.BuildTargetService;
import com.microsoft.java.bs.core.internal.services.LifecycleService;
import com.microsoft.java.bs.core.internal.transport.NamedPipeStream;

import ch.epfl.scala.bsp4j.BuildClient;
import org.apache.commons.lang3.StringUtils;

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

    org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> launcher;
    Map<String, String> params = parseArgs(args);
    String pipePath = params.get("pipe");
    if (StringUtils.isNotBlank(pipePath)) {
      launcher = createLauncherUsingPipe(pipePath);
    } else {
      launcher = createLauncherUsingStdIo();
    }

    setupLoggers(launcher.getRemoteProxy());
    launcher.startListening();
  }

  private static org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> 
      createLauncherUsingPipe(String pipePath) {
    NamedPipeStream pipeStream = new NamedPipeStream(pipePath);
    try {
      return createLauncher(pipeStream.getOutputStream(), pipeStream.getInputStream());
    } catch (IOException e) {
      throw new IllegalStateException("Error initializing the named pipe", e);
    }
  }

  private static org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> createLauncherUsingStdIo() {
    return createLauncher(System.out, System.in);
  }

  private static org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> 
      createLauncher(OutputStream outputStream, InputStream inputStream) {
    BuildTargetManager buildTargetManager = new BuildTargetManager();
    PreferenceManager preferenceManager = new PreferenceManager();
    GradleApiConnector connector = new GradleApiConnector(preferenceManager);
    LifecycleService lifecycleService = new LifecycleService(connector, preferenceManager);
    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
        connector, preferenceManager);
    GradleBuildServer gradleBuildServer = new GradleBuildServer(lifecycleService,
        buildTargetService);
    org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> launcher = new 
        org.eclipse.lsp4j.jsonrpc.Launcher.Builder<BuildClient>()
        .setOutput(outputStream)
        .setInput(inputStream)
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

  /**
   * Parse the arguments and return a map of key-value pairs.
   */
  public static Map<String, String> parseArgs(String[] args) {
    Map<String, String> paramMap = new HashMap<>();
    for (String arg : args) {
      if (arg.startsWith("--")) {
        int index = arg.indexOf('=');
        if (index != -1) {
          String key = arg.substring(2, index);
          String value = arg.substring(index + 1);
          paramMap.put(key, value);
        }
      }
    }
    return paramMap;
  }
}
