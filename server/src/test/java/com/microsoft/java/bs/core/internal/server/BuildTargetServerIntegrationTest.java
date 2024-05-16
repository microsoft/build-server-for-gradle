// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildClientCapabilities;
import ch.epfl.scala.bsp4j.BuildServer;
import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileReport;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.DidChangeBuildTarget;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.JavaBuildServer;
import ch.epfl.scala.bsp4j.JvmBuildServer;
import ch.epfl.scala.bsp4j.LogMessageParams;
import ch.epfl.scala.bsp4j.MavenDependencyModule;
import ch.epfl.scala.bsp4j.MavenDependencyModuleArtifact;
import ch.epfl.scala.bsp4j.MessageType;
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams;
import ch.epfl.scala.bsp4j.ShowMessageParams;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import ch.epfl.scala.bsp4j.TaskStartParams;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.Launcher;
import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.services.BuildTargetService;
import com.microsoft.java.bs.core.internal.services.LifecycleService;
import com.microsoft.java.bs.core.internal.utils.JsonUtils;
import com.microsoft.java.bs.gradle.model.SupportedLanguages;

// TODO: Move to a dedicated source set for integration tests
class BuildTargetServerIntegrationTest {

  private interface TestServer extends BuildServer, JavaBuildServer, JvmBuildServer {
  }

  private static class TestClient implements BuildClient {

    private final List<TaskStartParams> startReports = new ArrayList<>();
    private final List<TaskFinishParams> finishReports = new ArrayList<>();
    private final List<CompileReport> compileReports = new ArrayList<>();
    private final List<CompileResult> compileResults = new ArrayList<>();
    private final List<LogMessageParams> logMessages = new ArrayList<>();

    void clearMessages() {
      startReports.clear();
      finishReports.clear();
      compileReports.clear();
      compileResults.clear();
      logMessages.clear();
    }

    void waitOnStartReports(int size) {
      waitOnMessages("Start Reports", size, startReports::size);
    }

    void waitOnFinishReports(int size) {
      waitOnMessages("Finish Reports", size, finishReports::size);
    }

    void waitOnCompileReports(int size) {
      waitOnMessages("Compile Reports", size, compileReports::size);
    }

    void waitOnCompileResults(int size) {
      waitOnMessages("Compile Results", size, compileResults::size);
    }

    void waitOnLogMessages(int size) {
      waitOnMessages("Log Messages", size, logMessages::size);
    }

    long finishReportErrorCount() {
      return finishReports.stream()
          .filter(report -> report.getStatus() == StatusCode.ERROR)
          .count();
    }

    private void waitOnMessages(String message, int size, IntSupplier sizeSupplier) {
      // set to 5000ms because it seems reasonable
      long timeoutMs = 5000;
      long endTime = System.currentTimeMillis() + timeoutMs;
      while (sizeSupplier.getAsInt() < size
              && System.currentTimeMillis() < endTime) {
        synchronized (this) {
          long waitTime = endTime - System.currentTimeMillis();
          if (waitTime > 0) {
            try {
              wait(waitTime);
            } catch (InterruptedException e) {
              // do nothing
            }
          }
        }
      }
      assertEquals(size, sizeSupplier.getAsInt(), message + " count error");
    }

    private CompileReport findCompileReport(BuildTargetIdentifier btId) {
      CompileReport compileReport = compileReports.stream()
              .filter(report -> report.getTarget().equals(btId))
              .findFirst()
              .orElse(null);
      assertNotNull(compileReport, () -> {
        String availableTargets = compileReports.stream()
                .map(report -> report.getTarget().toString())
                .collect(Collectors.joining(", "));
        return "Target not found " + btId + ". Available: " + availableTargets;
      });
      return compileReport;
    }

    @Override
    public void onBuildShowMessage(ShowMessageParams params) {
      // do nothing
    }

    @Override
    public void onBuildLogMessage(LogMessageParams params) {
      logMessages.add(params);
      synchronized (this) {
        notify();
      }
    }

    @Override
    public void onBuildTaskStart(TaskStartParams params) {
      startReports.add(params);
      synchronized (this) {
        notify();
      }
    }

    @Override
    public void onBuildTaskProgress(TaskProgressParams params) {
      // do nothing
    }

    @Override
    public void onBuildTaskFinish(TaskFinishParams params) {
      if (params.getDataKind() != null) {
        if (params.getDataKind().equals("compile-report")) {
          compileReports.add(JsonUtils.toModel(params.getData(), CompileReport.class));
        } else if (params.getDataKind().equals("compile-result")) {
          compileResults.add(JsonUtils.toModel(params.getData(), CompileResult.class));
        }
      }
      finishReports.add(params);
      synchronized (this) {
        notify();
      }
    }

    @Override
    public void onBuildPublishDiagnostics(PublishDiagnosticsParams params) {
      // do nothing
    }

    @Override
    public void onBuildTargetDidChange(DidChangeBuildTarget params) {
      // do nothing
    }
  }

  @BeforeAll
  static void beforeClass() {
    String pluginDir = Paths.get(System.getProperty("user.dir"),
        "build", "libs", "plugins").toString();
    System.setProperty(Launcher.PROP_PLUGIN_DIR, pluginDir);
    System.setProperty("bsp.plugin.reloadworkspace.disabled", "true");
  }

  @AfterAll
  static void afterClass() {
    System.clearProperty(Launcher.PROP_PLUGIN_DIR);
    System.clearProperty("bsp.plugin.reloadworkspace.disabled");
  }

  private InitializeBuildParams getInitializeBuildParams(String projectDir) {
    File root = Paths.get(
        System.getProperty("user.dir"),
        "..",
        "testProjects",
        projectDir).toFile();

    BuildClientCapabilities capabilities =
        new BuildClientCapabilities(SupportedLanguages.allBspNames);
    return new InitializeBuildParams(
        "test-client",
        "0.1.0",
        "0.1.0",
        root.toURI().toString(),
        capabilities
    );
  }

  private void withNewTestServer(String project, BiConsumer<TestServer, TestClient> consumer) {
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
      // server
      BuildTargetManager buildTargetManager = new BuildTargetManager();
      PreferenceManager preferenceManager = new PreferenceManager();
      GradleApiConnector connector = new GradleApiConnector(preferenceManager);
      LifecycleService lifecycleService = new LifecycleService(connector, preferenceManager);
      BuildTargetService buildTargetService = new BuildTargetService(buildTargetManager,
              connector, preferenceManager);
      GradleBuildServer gradleBuildServer =
              new GradleBuildServer(lifecycleService, buildTargetService);
      org.eclipse.lsp4j.jsonrpc.Launcher<BuildClient> serverLauncher =
              new org.eclipse.lsp4j.jsonrpc.Launcher.Builder<BuildClient>()
                      .setLocalService(gradleBuildServer)
                      .setRemoteInterface(BuildClient.class)
                      .setOutput(serverOut)
                      .setInput(serverIn)
                      .setExecutorService(threadPool)
                      .create();
      buildTargetService.setClient(serverLauncher.getRemoteProxy());
      // client
      TestClient client = new TestClient();
      org.eclipse.lsp4j.jsonrpc.Launcher<TestServer> clientLauncher =
          new org.eclipse.lsp4j.jsonrpc.Launcher.Builder<TestServer>()
              .setLocalService(client)
              .setRemoteInterface(TestServer.class)
              .setInput(clientIn)
              .setOutput(clientOut)
              .setExecutorService(threadPool)
              .create();
      // start
      clientLauncher.startListening();
      serverLauncher.startListening();
      TestServer testServer = clientLauncher.getRemoteProxy();
      try {
        InitializeBuildParams params = getInitializeBuildParams(project);
        testServer.buildInitialize(params).join();
        testServer.onBuildInitialized();
        consumer.accept(testServer, client);
      } finally {
        testServer.buildShutdown().join();
        threadPool.shutdown();
      }
    } catch (IOException e) {
      throw new IllegalStateException("Error closing streams", e);
    }
  }

  @Test
  void testCompilingSingleProjectServer() {
    withNewTestServer("junit5-jupiter-starter-gradle", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());
      assertEquals(2, btIds.size());
      client.waitOnStartReports(1);
      client.waitOnFinishReports(1);
      client.waitOnCompileReports(0);
      client.waitOnCompileResults(0);
      client.waitOnLogMessages(0);
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      client.clearMessages();

      // check dependency sources
      DependencySourcesParams dependencySourcesParams = new DependencySourcesParams(btIds);
      DependencySourcesResult dependencySourcesResult = gradleBuildServer
          .buildTargetDependencySources(dependencySourcesParams).join();
      assertEquals(2, dependencySourcesResult.getItems().size());
      List<String> allSources = dependencySourcesResult.getItems().stream()
          .flatMap(item -> item.getSources().stream()).collect(Collectors.toList());
      assertTrue(allSources.stream().anyMatch(source -> source.endsWith("-sources.jar")));

      // check dependency modules
      DependencyModulesParams dependencyModulesParams = new DependencyModulesParams(btIds);
      DependencyModulesResult dependencyModulesResult = gradleBuildServer
              .buildTargetDependencyModules(dependencyModulesParams).join();
      assertEquals(2, dependencyModulesResult.getItems().size());
      List<MavenDependencyModuleArtifact> allArtifacts = dependencyModulesResult.getItems().stream()
              .flatMap(item -> item.getModules().stream())
              .filter(dependencyModule -> "maven".equals(dependencyModule.getDataKind()))
              .map(dependencyModule -> JsonUtils.toModel(dependencyModule.getData(),
                  MavenDependencyModule.class))
              .flatMap(mavenDependencyModule -> mavenDependencyModule.getArtifacts().stream())
              .filter(artifact -> "sources".equals(artifact.getClassifier()))
              .collect(Collectors.toList());
      assertTrue(allArtifacts.stream().anyMatch(artifact ->
          artifact.getUri().endsWith("-sources.jar")));

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      CleanCacheResult cleanResult = gradleBuildServer
          .buildTargetCleanCache(cleanCacheParams).join();
      assertTrue(cleanResult.getCleaned());
      client.waitOnStartReports(1);
      client.waitOnFinishReports(1);
      client.waitOnCompileReports(0);
      client.waitOnCompileResults(0);
      client.waitOnLogMessages(0);
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      client.clearMessages();

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      CompileResult compileResult = gradleBuildServer.buildTargetCompile(compileParams).join();
      assertEquals(StatusCode.OK, compileResult.getStatusCode());
      client.waitOnStartReports(6);
      client.waitOnFinishReports(6);
      client.waitOnCompileReports(6);
      client.waitOnCompileResults(0);
      client.waitOnLogMessages(0);
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      for (BuildTargetIdentifier btId : btIds) {
        CompileReport compileReport = client.findCompileReport(btId);
        assertEquals("originId", compileReport.getOriginId());
        // TODO compile results are not yet implemented so always zero for now.
        assertEquals(0, compileReport.getWarnings());
        assertEquals(0, compileReport.getErrors());
      }
      client.clearMessages();
    });
  }

  @Test
  void testFailingServer() {
    withNewTestServer("fail-compilation", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());
      assertEquals(2, btIds.size());
      client.waitOnStartReports(1);
      client.waitOnFinishReports(1);
      client.waitOnCompileReports(0);
      client.waitOnCompileResults(0);
      client.waitOnLogMessages(0);
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      client.clearMessages();

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      CleanCacheResult cleanResult = gradleBuildServer
          .buildTargetCleanCache(cleanCacheParams).join();
      assertTrue(cleanResult.getCleaned());
      client.waitOnStartReports(1);
      client.waitOnFinishReports(1);
      client.waitOnCompileReports(0);
      client.waitOnCompileResults(0);
      client.waitOnLogMessages(0);
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      client.clearMessages();

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      CompileResult compileResult = gradleBuildServer.buildTargetCompile(compileParams).join();
      assertEquals(StatusCode.ERROR, compileResult.getStatusCode());
      client.waitOnStartReports(4);
      client.waitOnFinishReports(4);
      client.waitOnCompileReports(4);
      client.waitOnCompileResults(0);
      client.waitOnLogMessages(1);
      assertEquals(1, client.finishReportErrorCount());
      for (BuildTargetIdentifier btId : btIds) {
        CompileReport compileReport = client.findCompileReport(btId);
        assertEquals("originId", compileReport.getOriginId());
        // TODO compile results are not yet implemented so always zero for now.
        assertEquals(0, compileReport.getWarnings());
        assertEquals(0, compileReport.getErrors());
      }
      for (LogMessageParams message : client.logMessages) {
        assertEquals("originId", message.getOriginId());
        assertEquals(MessageType.ERROR, message.getType());
      }
      client.clearMessages();
    });
  }
}
