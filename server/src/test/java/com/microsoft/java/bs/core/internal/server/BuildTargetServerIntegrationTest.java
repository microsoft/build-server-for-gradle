// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.MavenDependencyModule;
import ch.epfl.scala.bsp4j.MavenDependencyModuleArtifact;
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
import ch.epfl.scala.bsp4j.DidChangeBuildTarget;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.JavaBuildServer;
import ch.epfl.scala.bsp4j.JvmBuildServer;
import ch.epfl.scala.bsp4j.LogMessageParams;
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams;
import ch.epfl.scala.bsp4j.ShowMessageParams;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import ch.epfl.scala.bsp4j.TaskStartParams;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

// TODO: Move to a dedicated source set for integration tests
class BuildTargetServerIntegrationTest {

  private interface TestServer extends BuildServer, JavaBuildServer, JvmBuildServer {
  }

  private static class TestClient implements BuildClient {

    private final List<TaskStartParams> startReports = new ArrayList<>();
    private final List<TaskFinishParams> finishReports = new ArrayList<>();
    private final Set<CompileReport> compileReports = new HashSet<>();

    void clearMessages() {
      startReports.clear();
      finishReports.clear();
      compileReports.clear();
    }

    void waitOnMessages(int startMessagesSize,
                        int finishMessagesSize,
                        int compileReportsSize) {
      // set to 5000ms because it seems reasonable
      long timeoutMs = 5000;
      long endTime = System.currentTimeMillis() + timeoutMs;
      while ((startReports.size() < startMessagesSize
              || finishReports.size() < finishMessagesSize
              || compileReports.size() < compileReportsSize)
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
      assertEquals(startMessagesSize, startReports.size(), "Start Report count error");
      assertEquals(finishMessagesSize, finishReports.size(), "Finish Reports count error");
      assertEquals(compileReportsSize, compileReports.size(), "Compile Reports count error");
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
      // do nothing
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
  }

  @AfterAll
  static void afterClass() {
    System.clearProperty(Launcher.PROP_PLUGIN_DIR);
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

  private BuildTargetIdentifier getBt(List<BuildTargetIdentifier> btIds,
      String projectPathSection, String sourceSetName) {
    String sourceSet = "?sourceset=" + sourceSetName;
    List<BuildTargetIdentifier> matching = btIds.stream()
            .filter(id -> id.getUri().contains(projectPathSection))
            .filter(id -> id.getUri().endsWith(sourceSet))
            .collect(Collectors.toList());
    assertFalse(matching.isEmpty(), () -> "Build Target " + projectPathSection
            + " with source set " + sourceSetName + " not found in "
            + btIds.stream().map(Object::toString).collect(Collectors.joining(", ")));
    assertEquals(1, matching.size(), () -> "Too many Build Targets like " + projectPathSection
            + " with source set " + sourceSetName + " found in "
            + btIds.stream().map(Object::toString).collect(Collectors.joining(", ")));
    return matching.get(0);
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
      client.waitOnMessages(2, 2, 1);
      client.clearMessages();

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      CompileResult compileResult = gradleBuildServer.buildTargetCompile(compileParams).join();
      assertEquals(StatusCode.OK, compileResult.getStatusCode());
      client.waitOnMessages(2, 2, 1);
      BuildTargetIdentifier testBt = getBt(btIds, "/junit5-jupiter-starter-gradle/", "test");
      CompileReport compileReportTest = client.findCompileReport(testBt);
      assertEquals(0, compileReportTest.getWarnings());
      assertEquals(0, compileReportTest.getErrors());
      client.clearMessages();
    });
  }
}
