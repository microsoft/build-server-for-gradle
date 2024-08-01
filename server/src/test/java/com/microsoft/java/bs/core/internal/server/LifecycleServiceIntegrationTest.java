package com.microsoft.java.bs.core.internal.server;

import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.MessageType;
import ch.epfl.scala.bsp4j.ShowMessageParams;
import com.microsoft.java.bs.core.internal.model.Preferences;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LifecycleServiceIntegrationTest extends IntegrationTest {

  private static InitializeBuildParams getInitializedBuildParamsWithJdk(
      String projectDir,
      String jdkVersion,
      String gradleJavaVersionPath
  ) {

    Preferences preferences = new Preferences();
    var jdks = new HashMap<String, String>();
    jdks.put(jdkVersion, "file:///tmp/nonexistent_file.txt");
    preferences.setJdks(jdks);
    preferences.setGradleJavaHome(gradleJavaVersionPath);

    InitializeBuildParams initParams = getInitializeBuildParams(projectDir);
    initParams.setData(preferences);
    return initParams;

  }

  @Test
  void testCompatibleDefaultJavaHomeProjectServer() {

    ExecutorService threadPool = Executors.newCachedThreadPool();
    try (PipedInputStream clientIn = new PipedInputStream();
         PipedOutputStream clientOut = new PipedOutputStream();
         PipedInputStream serverIn = new PipedInputStream();
         PipedOutputStream serverOut = new PipedOutputStream()) {
      try {
        clientIn.connect(serverOut);
        clientOut.connect(serverIn);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot setup streams", e);
      }
      var pair = setupClientServer(clientIn, clientOut, serverIn, serverOut, threadPool);
      TestClient client = pair.getLeft();
      TestServer testServer = pair.getRight();
      try {

        InitializeBuildParams initParams = getInitializeBuildParams("legacy-gradle");
        testServer.buildInitialize(initParams).join();

        // Wait for the configuration logics to complete
        synchronized (this) {
          wait(5000L);
        }

        assertEquals(0, client.showMessages.size());

      } catch (InterruptedException e) {
        throw new RuntimeException("Thread interrupted during wait.");
      } finally {
        testServer.buildShutdown().join();
        threadPool.shutdown();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error closing streams", e);
    }

  }

  @Test
  void testCompatibleUserJavaHomeProjectServer() {

    ExecutorService threadPool = Executors.newCachedThreadPool();
    try (PipedInputStream clientIn = new PipedInputStream();
         PipedOutputStream clientOut = new PipedOutputStream();
         PipedInputStream serverIn = new PipedInputStream();
         PipedOutputStream serverOut = new PipedOutputStream()) {
      try {
        clientIn.connect(serverOut);
        clientOut.connect(serverIn);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot setup streams", e);
      }
      var pair = setupClientServer(clientIn, clientOut, serverIn, serverOut, threadPool);
      TestClient client = pair.getLeft();
      TestServer testServer = pair.getRight();
      try {

        InitializeBuildParams initParams =
            getInitializedBuildParamsWithJdk("Non-Existent Project", "1.8", null);

        testServer.buildInitialize(initParams).join();
        client.waitOnShowMessages(1);
        ShowMessageParams param = client.showMessages.get(0);
        assertEquals(MessageType.INFORMATION, param.getType());
        assertTrue(param.getMessage()
            .startsWith("Default JDK wasn't compatible with current gradle version"));
        testServer.onBuildInitialized();
        client.clearMessages();

      } finally {
        testServer.buildShutdown().join();
        threadPool.shutdown();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error closing streams", e);
    }

  }
  
  @Test
  void testIncompatibleUserJavaHomeProjectServer() {

    ExecutorService threadPool = Executors.newCachedThreadPool();
    try (PipedInputStream clientIn = new PipedInputStream();
         PipedOutputStream clientOut = new PipedOutputStream();
         PipedInputStream serverIn = new PipedInputStream();
         PipedOutputStream serverOut = new PipedOutputStream()) {
      try {
        clientIn.connect(serverOut);
        clientOut.connect(serverIn);
      } catch (IOException e) {
        throw new IllegalStateException("Cannot setup streams", e);
      }
      var pair = setupClientServer(clientIn, clientOut, serverIn, serverOut, threadPool);
      TestClient client = pair.getLeft();
      TestServer testServer = pair.getRight();
      try {

        InitializeBuildParams initParams =
            getInitializedBuildParamsWithJdk(
                "Non-Existent Project",
                "23.0.1",
                "file:///tmp/nonexistent_file.txt"
            );

        testServer.buildInitialize(initParams).join();
        client.waitOnShowMessages(1);
        ShowMessageParams param = client.showMessages.get(0);
        assertEquals(MessageType.ERROR, param.getType());
        assertTrue(param.getMessage()
                .startsWith("Failed to find a JDK compatible with current gradle version"));
        testServer.onBuildInitialized();
        client.clearMessages();

      } finally {
        testServer.buildShutdown().join();
        threadPool.shutdown();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error closing streams", e);
    }

  }

}
