package com.microsoft.java.bs.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.server.GradleBuildServer;
import com.microsoft.java.bs.core.internal.services.BuildTargetService;
import com.microsoft.java.bs.core.internal.services.LifecycleService;

import ch.epfl.scala.bsp4j.BuildClient;

/**
 * Main entry point for the BSP server.
 */
public class Launcher {
  
  /**
   * The property name for the build server storage location.
   */
  static final String PROP_BUILD_SERVER_STORAGE = "server.storage";

  /**
   * The property name for the directory location storing the plugin and init script.
   */
  static final String PROP_PLUGIN_DIR = "plugin.dir";

  private static final Logger logger = LoggerFactory.getLogger(Launcher.class);

  /**
   * Main entry point.
   */
  public static void main(String[] args) {
    checkRequiredProperties();
    logSessionStart();

    org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> launcher = createLauncher();
    launcher.startListening();
  }

  private static org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> createLauncher() {
    BuildTargetManager buildTargetManager = new BuildTargetManager();
    LifecycleService lifecycleService = new LifecycleService(buildTargetManager);
    BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager);
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
    if (System.getProperty(PROP_BUILD_SERVER_STORAGE) == null) {
      throw new IllegalStateException("The property '"
          + PROP_BUILD_SERVER_STORAGE + "' is not set");
    }

    if (System.getProperty(PROP_PLUGIN_DIR) == null) {
      throw new IllegalStateException("The property '" + PROP_PLUGIN_DIR + "' is not set");
    }
  }

  private static void logSessionStart() {
    LocalDateTime currentTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    String formattedTimestamp = currentTime.format(formatter);
    logger.info("!SESSION {}\n---------------------------------------------------\n",
        formattedTimestamp);
  }
}
