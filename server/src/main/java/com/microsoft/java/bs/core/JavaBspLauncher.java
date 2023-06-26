package com.microsoft.java.bs.core;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.microsoft.java.bs.core.bsp.BspServer;
import com.microsoft.java.bs.core.managers.ParentProcessWatcher;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildServer;

/**
 * Main entry point for the BSP server.
 */
public class JavaBspLauncher {

  private static final String PROP_BUILD_SERVER_STORAGE = "buildServerStorage";

  private static final Logger logger = LoggerFactory.getLogger(JavaBspLauncher.class);

  private static Injector injector;

  public static BuildClient client;

  /**
   * Main entry point.
   */
  public static void main(String[] args) {
    logSessionStart();
    checkRequiredProperties();
    injector = Guice.createInjector(new BspModule());
    Launcher<BuildClient> launcher = createLauncher();
    client = launcher.getRemoteProxy();
    launcher.startListening();
  }

  private static Launcher<BuildClient> createLauncher() {
    BuildServer bspServer = injector.getInstance(BspServer.class);
    // TODO: change the thread pool
    ExecutorService fixedThreadPool = Executors.newFixedThreadPool(1);
    return new Launcher.Builder<BuildClient>()
      .setOutput(System.out)
      .setInput(System.in)
      .setLocalService(bspServer)
      .setRemoteInterface(BuildClient.class)
      .setExecutorService(fixedThreadPool)
      .wrapMessages(new ParentProcessWatcher(bspServer))
      .create();
  }

  private static void logSessionStart() {
    LocalDateTime currentTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    String formattedTimestamp = currentTime.format(formatter);
    logger.info("\n\n!SESSION {} -----------------------------------------------",
        formattedTimestamp);
  }

  private static void checkRequiredProperties() {
    if (System.getProperty(PROP_BUILD_SERVER_STORAGE) == null) {
      throw new IllegalStateException("The property 'buildServerStorage' is not set");
    }
  }
}
