package com.microsoft.java.bs.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.eclipse.lsp4j.jsonrpc.Launcher;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.microsoft.java.bs.core.bsp.BspServer;
import com.microsoft.java.bs.core.handlers.ExceptionHandler;
import com.microsoft.java.bs.core.managers.ParentProcessWatcher;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildServer;

/**
 * Main entry point for the BSP server.
 */
public class JavaBspLauncher {

  private static Injector injector;

  public static BuildClient client;

  /**
   * Main entry point.
   */
  public static void main(String[] args) {
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
      .setExceptionHandler(new ExceptionHandler())
      .create();
  }
}
