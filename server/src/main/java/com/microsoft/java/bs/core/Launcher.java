package com.microsoft.java.bs.core;

import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

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

  public static BuildClient client;

  public static final Logger LOGGER = Logger.getLogger("GradleBuildServerLogger");

  /**
   * The property name for the build server storage location.
   */
  static final String PROP_BUILD_SERVER_STORAGE = "server.storage";

  /**
   * The property name for the directory location storing the plugin and init script.
   */
  static final String PROP_PLUGIN_DIR = "plugin.dir";

  /**
   * Main entry point.
   */
  public static void main(String[] args) {
    checkRequiredProperties();
    setupLoggers();

    org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> launcher = createLauncher();
    client = launcher.getRemoteProxy();
    launcher.startListening();
  }

  private static org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> createLauncher() {
    BuildTargetManager buildTargetManager = new BuildTargetManager();
    PreferenceManager preferenceManager = new PreferenceManager();
    LifecycleService lifecycleService = new LifecycleService(buildTargetManager, preferenceManager);
    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
        preferenceManager);
    GradleBuildServer gradleBuildServer = new GradleBuildServer(lifecycleService,
        buildTargetService);
    return new org.eclipse.lsp4j.jsonrpc.Launcher.Builder<BuildClient>()
      .setOutput(System.out)
      .setInput(System.in)
      .setLocalService(gradleBuildServer)
      .setRemoteInterface(BuildClient.class)
      .setExecutorService(Executors.newCachedThreadPool())
      .create();
  }

  private static void checkRequiredProperties() {
    if (System.getProperty(PROP_PLUGIN_DIR) == null) {
      throw new IllegalStateException("The property '" + PROP_PLUGIN_DIR + "' is not set");
    }
  }

  private static void setupLoggers() {
    LOGGER.setUseParentHandlers(false);
    LogHandler logHandler = new LogHandler();
    logHandler.setLevel(Level.FINE);
    LOGGER.addHandler(logHandler);

    if (System.getProperty("disableServerTelemetry") == null) {
      TelemetryHandler telemetryHandler = new TelemetryHandler();
      telemetryHandler.setLevel(Level.INFO);
      LOGGER.addHandler(telemetryHandler);
    }
  }
}
