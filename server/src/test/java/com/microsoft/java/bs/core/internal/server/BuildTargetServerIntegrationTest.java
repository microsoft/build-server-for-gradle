// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
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
import ch.epfl.scala.bsp4j.CompileTask;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.DidChangeBuildTarget;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.JavaBuildServer;
import ch.epfl.scala.bsp4j.JvmBuildServer;
import ch.epfl.scala.bsp4j.JvmEnvironmentItem;
import ch.epfl.scala.bsp4j.JvmMainClass;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult;
import ch.epfl.scala.bsp4j.LogMessageParams;
import ch.epfl.scala.bsp4j.MavenDependencyModule;
import ch.epfl.scala.bsp4j.MavenDependencyModuleArtifact;
import ch.epfl.scala.bsp4j.MessageType;
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams;
import ch.epfl.scala.bsp4j.RunParams;
import ch.epfl.scala.bsp4j.RunParamsDataKind;
import ch.epfl.scala.bsp4j.RunResult;
import ch.epfl.scala.bsp4j.ScalaMainClass;
import ch.epfl.scala.bsp4j.ScalaTestClassesItem;
import ch.epfl.scala.bsp4j.ScalaTestParams;
import ch.epfl.scala.bsp4j.ShowMessageParams;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import ch.epfl.scala.bsp4j.TaskStartParams;
import ch.epfl.scala.bsp4j.TestFinish;
import ch.epfl.scala.bsp4j.TestParams;
import ch.epfl.scala.bsp4j.TestParamsDataKind;
import ch.epfl.scala.bsp4j.TestReport;
import ch.epfl.scala.bsp4j.TestResult;
import ch.epfl.scala.bsp4j.TestStart;
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
    private final List<CompileTask> compileTasks = new ArrayList<>();
    private final List<CompileReport> compileReports = new ArrayList<>();
    private final List<LogMessageParams> logMessages = new ArrayList<>();
    private final List<TestReport> testReports = new ArrayList<>();
    private final List<TestStart> testStarts = new ArrayList<>();
    private final List<TestFinish> testFinishes = new ArrayList<>();

    void clearMessages() {
      startReports.clear();
      finishReports.clear();
      compileTasks.clear();
      compileReports.clear();
      logMessages.clear();
      testReports.clear();
      testStarts.clear();
      testFinishes.clear();
    }

    void waitOnStartReports(int size) {
      waitOnMessages("Start Reports", size, startReports::size);
    }

    void waitOnFinishReports(int size) {
      waitOnMessages("Finish Reports", size, finishReports::size);
    }

    void waitOnCompileTasks(int size) {
      waitOnMessages("Compile Tasks", size, compileTasks::size);
    }

    void waitOnCompileReports(int size) {
      waitOnMessages("Compile Reports", size, compileReports::size);
    }

    void waitOnLogMessages(int size) {
      waitOnMessages("Log Messages", size, logMessages::size);
    }

    void waitOnTestReports(int size) {
      waitOnMessages("Test Reports", size, testReports::size);
    }

    void waitOnTestStarts(int size) {
      waitOnMessages("Test Starts", size, testStarts::size);
    }

    void waitOnTestFinishes(int size) {
      waitOnMessages("Test Finishes", size, testFinishes::size);
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
      if (params.getDataKind() != null) {
        if (params.getDataKind().equals("compile-task")) {
          compileTasks.add(JsonUtils.toModel(params.getData(), CompileTask.class));
        } else if (params.getDataKind().equals("test-start")) {
          testStarts.add(JsonUtils.toModel(params.getData(), TestStart.class));
        } else {
          fail("Task Start kind not handled " + params.getDataKind());
        }
      }
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
        } else if (params.getDataKind().equals("test-report")) {
          testReports.add(JsonUtils.toModel(params.getData(), TestReport.class));
        } else if (params.getDataKind().equals("test-finish")) {
          testFinishes.add(JsonUtils.toModel(params.getData(), TestFinish.class));
        } else {
          fail("Task Finish kind not handled " + params.getDataKind());
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

  private static BuildTargetIdentifier findTarget(List<BuildTarget> targets,
      String displayName) {
    Optional<BuildTarget> matchingTargets = targets.stream()
            .filter(res -> displayName.equals(res.getDisplayName()))
            .findAny();
    assertFalse(matchingTargets.isEmpty(), () -> {
      List<String> targetNames = targets.stream()
              .map(BuildTarget::getDisplayName)
              .collect(Collectors.toList());
      return "Target " + displayName + " not found in " + targetNames;
    });
    return matchingTargets.get().getId();
  }

  private static JvmEnvironmentItem findTest(
      JvmTestEnvironmentResult testEnvResult, String mainClass) {
    List<JvmEnvironmentItem> tests = testEnvResult.getItems().stream()
            .filter(res -> res.getMainClasses().stream()
                .anyMatch(main -> main.getClassName().equals(mainClass)))
            .collect(Collectors.toList());
    assertFalse(tests.isEmpty(), () -> {
      List<String> classes = testEnvResult.getItems().stream()
              .flatMap(res -> res.getMainClasses().stream()
                      .map(JvmMainClass::getClassName))
              .collect(Collectors.toList());
      return "Test " + mainClass + " not found in " + classes;
    });
    return tests.get(0);
  }

  @Test
  void testAllOnSingleProjectServer() {
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
      client.waitOnCompileTasks(0);
      client.waitOnCompileReports(0);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(0);
      client.waitOnTestFinishes(0);
      client.waitOnTestReports(0);
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
      client.waitOnCompileTasks(0);
      client.waitOnCompileReports(0);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(0);
      client.waitOnTestFinishes(0);
      client.waitOnTestReports(0);
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      client.clearMessages();

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      CompileResult compileResult = gradleBuildServer.buildTargetCompile(compileParams).join();
      assertEquals(StatusCode.OK, compileResult.getStatusCode());
      client.waitOnStartReports(2);
      client.waitOnFinishReports(2);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(0);
      client.waitOnTestFinishes(0);
      client.waitOnTestReports(0);
      for (CompileReport message : client.compileReports) {
        assertFalse(message.getNoOp());
      }
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

      // retrieve test names
      JvmTestEnvironmentParams testEnvParams = new JvmTestEnvironmentParams(btIds);
      JvmTestEnvironmentResult testEnvResult =
              gradleBuildServer.jvmTestEnvironment(testEnvParams).join();
      JvmEnvironmentItem calculatorTestsItem =
          findTest(testEnvResult, "com.example.project.CalculatorTests");
      assertFalse(calculatorTestsItem.getMainClasses().isEmpty());
      assertFalse(calculatorTestsItem.getClasspath().isEmpty());
      JvmEnvironmentItem failingTestsItem =
          findTest(testEnvResult, "com.example.project.FailingTests");
      assertFalse(failingTestsItem.getMainClasses().isEmpty());
      assertFalse(failingTestsItem.getClasspath().isEmpty());
      client.waitOnStartReports(11);
      client.waitOnFinishReports(11);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(0);
      client.waitOnTestFinishes(0);
      client.waitOnTestReports(0);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      client.clearMessages();

      // run passing tests
      List<String> calculatorMainClasses = new LinkedList<>();
      calculatorMainClasses.add("com.example.project.CalculatorTests");
      ScalaTestClassesItem calculatorTestClassesItem =
          new ScalaTestClassesItem(calculatorTestsItem.getTarget(), calculatorMainClasses);
      List<ScalaTestClassesItem> calculatorTestClasses = new LinkedList<>();
      calculatorTestClasses.add(calculatorTestClassesItem);
      ScalaTestParams calculatorScalaTestParams = new ScalaTestParams();
      calculatorScalaTestParams.setTestClasses(calculatorTestClasses);
      TestParams calculatorTestParams = new TestParams(btIds);
      calculatorTestParams.setOriginId("originId");
      calculatorTestParams.setDataKind(TestParamsDataKind.SCALA_TEST);
      calculatorTestParams.setData(calculatorScalaTestParams);
      TestResult calculatorTestResult =
          gradleBuildServer.buildTargetTest(calculatorTestParams).join();
      assertEquals(StatusCode.OK, calculatorTestResult.getStatusCode());
      assertEquals("originId", calculatorTestResult.getOriginId());
      // there are 5 tests in this class
      client.waitOnStartReports(7);
      client.waitOnFinishReports(8);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(5);
      client.waitOnTestFinishes(5);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      TestReport calculatorTestsReport = client.testReports.get(0);
      assertEquals(5, calculatorTestsReport.getPassed());
      assertEquals(0, calculatorTestsReport.getCancelled());
      assertEquals(0, calculatorTestsReport.getFailed());
      assertEquals(0, calculatorTestsReport.getIgnored());
      assertEquals(0, calculatorTestsReport.getSkipped());
      client.clearMessages();

      // run failing tests
      List<String> failingMainClasses = new LinkedList<>();
      failingMainClasses.add("com.example.project.FailingTests");
      ScalaTestClassesItem failingTestClassesItem =
          new ScalaTestClassesItem(failingTestsItem.getTarget(), failingMainClasses);
      List<ScalaTestClassesItem> failingTestClasses = new LinkedList<>();
      failingTestClasses.add(failingTestClassesItem);
      ScalaTestParams failingScalaTestParams = new ScalaTestParams();
      failingScalaTestParams.setTestClasses(failingTestClasses);
      TestParams failingTestParams = new TestParams(btIds);
      failingTestParams.setOriginId("originId");
      failingTestParams.setDataKind(TestParamsDataKind.SCALA_TEST);
      failingTestParams.setData(failingScalaTestParams);
      TestResult failingTestResult = gradleBuildServer.buildTargetTest(failingTestParams).join();
      assertEquals(StatusCode.ERROR, failingTestResult.getStatusCode());
      assertEquals("originId", failingTestResult.getOriginId());
      // there is 1 test in this class
      client.waitOnStartReports(3);
      client.waitOnFinishReports(4);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(1);
      client.waitOnTestFinishes(1);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      assertEquals(2, client.finishReportErrorCount());
      TestReport failingTestsReport = client.testReports.get(0);
      assertEquals(0, failingTestsReport.getPassed());
      assertEquals(0, failingTestsReport.getCancelled());
      assertEquals(1, failingTestsReport.getFailed());
      assertEquals(0, failingTestsReport.getIgnored());
      assertEquals(0, failingTestsReport.getSkipped());
      TestFinish failingTestsFinish = client.testFinishes.get(0);
      // TODO there is no way in BSP to pass back stacktrace so it's in message
      failingTestsFinish.getMessage()
          .contains("at com.example.project.FailingTests.failingTest(FailingTests.java:21)");
      client.clearMessages();

      // run main
      ScalaMainClass mainClass = new ScalaMainClass("com.example.project.Calculator",
              Collections.emptyList(), Collections.emptyList());
      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(),
              "junit5-jupiter-starter-gradle [main]");
      RunParams runParams = new RunParams(btId);
      runParams.setOriginId("originId");
      runParams.setDataKind(RunParamsDataKind.SCALA_MAIN_CLASS);
      runParams.setData(mainClass);
      RunResult runResult = gradleBuildServer.buildTargetRun(runParams).join();
      assertEquals("originId", runResult.getOriginId());
      client.waitOnStartReports(2);
      client.waitOnFinishReports(2);
      client.waitOnCompileTasks(1);
      client.waitOnCompileReports(1);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(0);
      client.waitOnTestFinishes(0);
      client.waitOnTestReports(0);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      assertEquals(StatusCode.OK, runResult.getStatusCode(),
          () -> client.finishReports.stream().map(TaskFinishParams::getMessage)
                      .collect(Collectors.joining("\n")));
      client.clearMessages();
    });
  }
  
  @Test
  void testCleanStraightToFindTest() {
    withNewTestServer("junit5-jupiter-starter-gradle", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      gradleBuildServer.buildTargetCleanCache(cleanCacheParams).join();
      client.clearMessages();

      // a request to find tests straight after a clean should produce compile results/reports
      // retrieve test names
      JvmTestEnvironmentParams testEnvParams = new JvmTestEnvironmentParams(btIds);
      gradleBuildServer.jvmTestEnvironment(testEnvParams).join();
      client.waitOnStartReports(11);
      client.waitOnFinishReports(11);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(0);
      client.waitOnTestFinishes(0);
      client.waitOnTestReports(0);
      for (CompileReport message : client.compileReports) {
        assertFalse(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      client.clearMessages();
    });
  }
  
  @Test
  void testCleanStraightToTest() {
    withNewTestServer("junit5-jupiter-starter-gradle", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      gradleBuildServer.buildTargetCleanCache(cleanCacheParams).join();
      client.clearMessages();
      
      // a request to run tests straight after a clean should produce compile results/reports
      // run tests
      List<String> mainClasses = new LinkedList<>();
      mainClasses.add("com.example.project.CalculatorTests");
      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(),
              "junit5-jupiter-starter-gradle [test]");
      ScalaTestClassesItem scalaTestClassesItem =
              new ScalaTestClassesItem(btId, mainClasses);
      List<ScalaTestClassesItem> testClasses = new LinkedList<>();
      testClasses.add(scalaTestClassesItem);
      ScalaTestParams scalaTestParams = new ScalaTestParams();
      scalaTestParams.setTestClasses(testClasses);
      TestParams testParams = new TestParams(btIds);
      testParams.setOriginId("originId");
      testParams.setDataKind(TestParamsDataKind.SCALA_TEST);
      testParams.setData(scalaTestParams);
      TestResult testResult = gradleBuildServer.buildTargetTest(testParams).join();
      assertEquals(StatusCode.OK, testResult.getStatusCode());
      assertEquals("originId", testResult.getOriginId());
      // there are 5 tests in this project
      client.waitOnStartReports(7);
      client.waitOnFinishReports(8);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(5);
      client.waitOnTestFinishes(5);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertFalse(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      client.clearMessages();
    });
  }

  @Test
  void testCleanStraightToRun() {
    withNewTestServer("junit5-jupiter-starter-gradle", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      gradleBuildServer.buildTargetCleanCache(cleanCacheParams).join();
      client.clearMessages();
      
      // a request to run mainClass straight after a clean should produce compile results/reports
      // run main
      ScalaMainClass mainClass = new ScalaMainClass("com.example.project.Calculator",
          Collections.emptyList(), Collections.emptyList());
      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(),
            "junit5-jupiter-starter-gradle [main]");
      RunParams runParams = new RunParams(btId);
      runParams.setOriginId("originId");
      runParams.setDataKind(RunParamsDataKind.SCALA_MAIN_CLASS);
      runParams.setData(mainClass);
      RunResult runResult = gradleBuildServer.buildTargetRun(runParams).join();
      assertEquals("originId", runResult.getOriginId());
      client.waitOnStartReports(2);
      client.waitOnFinishReports(2);
      client.waitOnCompileTasks(1);
      client.waitOnCompileReports(1);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(0);
      client.waitOnTestFinishes(0);
      client.waitOnTestReports(0);
      for (CompileReport message : client.compileReports) {
        assertFalse(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      assertEquals(StatusCode.OK, runResult.getStatusCode(),
          () -> client.finishReports.stream().map(TaskFinishParams::getMessage)
                    .collect(Collectors.joining("\n")));
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
      client.waitOnCompileTasks(0);
      client.waitOnCompileReports(0);
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
      client.waitOnCompileTasks(0);
      client.waitOnCompileReports(0);
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
      client.waitOnStartReports(2);
      client.waitOnFinishReports(2);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(1);
      for (CompileReport message : client.compileReports) {
        assertFalse(message.getNoOp());
      }
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
