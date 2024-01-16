// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.IntSupplier;
import java.util.stream.Collectors;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.BuildServer;
import ch.epfl.scala.bsp4j.CompileReport;
import ch.epfl.scala.bsp4j.DidChangeBuildTarget;
import ch.epfl.scala.bsp4j.JavaBuildServer;
import ch.epfl.scala.bsp4j.JvmBuildServer;
import ch.epfl.scala.bsp4j.LogMessageParams;
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams;
import ch.epfl.scala.bsp4j.RunParams;
import ch.epfl.scala.bsp4j.RunParamsDataKind;
import ch.epfl.scala.bsp4j.RunResult;
import ch.epfl.scala.bsp4j.ScalaMainClass;
import ch.epfl.scala.bsp4j.ShowMessageParams;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import ch.epfl.scala.bsp4j.TaskStartParams;
import ch.epfl.scala.bsp4j.TestFinish;
import ch.epfl.scala.bsp4j.TestReport;
import com.microsoft.java.bs.core.internal.utils.JsonUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.Launcher;
import com.microsoft.java.bs.core.internal.gradle.GradleApiConnector;
import com.microsoft.java.bs.core.internal.managers.BuildTargetManager;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.services.BuildTargetService;
import com.microsoft.java.bs.core.internal.services.LifecycleService;

import ch.epfl.scala.bsp4j.BuildClientCapabilities;
import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.InitializeBuildParams;
import ch.epfl.scala.bsp4j.JvmEnvironmentItem;
import ch.epfl.scala.bsp4j.JvmMainClass;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentParams;
import ch.epfl.scala.bsp4j.JvmTestEnvironmentResult;
import ch.epfl.scala.bsp4j.ScalaTestClassesItem;
import ch.epfl.scala.bsp4j.ScalaTestParams;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TestParams;
import ch.epfl.scala.bsp4j.TestParamsDataKind;
import ch.epfl.scala.bsp4j.TestResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;

// TODO: Move to a dedicated source set for integration tests
class BuildTargetServerIntegrationTest {

  private org.eclipse.lsp4j.jsonrpc.Launcher<TestServer> clientLauncher;
  private TestClient client;

  private ExecutorService threadPool;
  private PipedOutputStream clientOut;
  private PipedOutputStream serverOut;

  private interface TestServer extends BuildServer, JavaBuildServer, JvmBuildServer {

  }

  private static class TestClient implements BuildClient {
    private final List<CompileReport> compileReports = new ArrayList<>();
    private final List<CompileResult> compileResults = new ArrayList<>();
    private final List<TestReport> testReports = new ArrayList<>();
    private final List<TestFinish> testFinishes = new ArrayList<>();
    private final List<String> genericReports = new ArrayList<>();

    void clearMessages() {
      compileReports.clear();
      compileResults.clear();
      testReports.clear();
      testFinishes.clear();
      genericReports.clear();
    }

    void waitOnCompileReports(int size) {
      waitOnMessages("Compile Reports", size, compileReports::size);
    }

    void waitOnCompileResults(int size) {
      waitOnMessages("Compile Results", size, compileResults::size);
    }

    void waitOnTestReports(int size) {
      waitOnMessages("Test Reports", size, testReports::size);
    }

    void waitOnTestFinishes(int size) {
      waitOnMessages("Test Finishes", size, testFinishes::size);
    }

    void waitOnGenericReports(int size) {
      waitOnMessages("Generic Reports", size, genericReports::size);
    }

    private void waitOnMessages(String message,
                        int size,
                        IntSupplier sizeSupplier) {
      long defaultTimeout = 10000;
      long endTime = System.currentTimeMillis() + defaultTimeout;
      while (sizeSupplier.getAsInt() != size
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
      // do nothing
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
        } else if (params.getDataKind().equals("test-report")) {
          testReports.add(JsonUtils.toModel(params.getData(), TestReport.class));
        } else if (params.getDataKind().equals("test-finish")) {
          testFinishes.add(JsonUtils.toModel(params.getData(), TestFinish.class));
        } else {
          genericReports.add(params.getMessage());
        }
      } else {
        genericReports.add(params.getMessage());
      }
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

  @BeforeEach
  void beforeEach() {
    threadPool = Executors.newCachedThreadPool();
    PipedInputStream clientIn = new PipedInputStream();
    clientOut = new PipedOutputStream();
    PipedInputStream serverIn = new PipedInputStream();
    serverOut = new PipedOutputStream();
    try {
      clientIn.connect(serverOut);
      clientOut.connect(serverIn);
    } catch (IOException e) {
      throw new IllegalStateException("Error in setting up streams", e);
    }
    // server
    BuildTargetManager buildTargetManager = new BuildTargetManager();
    PreferenceManager preferenceManager = new PreferenceManager();
    GradleApiConnector connector = new GradleApiConnector(preferenceManager);
    LifecycleService lifecycleService = new LifecycleService(buildTargetManager,
        connector, preferenceManager);
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
    client = new TestClient();
    clientLauncher = new org.eclipse.lsp4j.jsonrpc.Launcher.Builder<TestServer>()
            .setLocalService(client)
            .setRemoteInterface(TestServer.class)
            .setInput(clientIn)
            .setOutput(clientOut)
            .setExecutorService(threadPool)
            .create();
    // start
    clientLauncher.startListening();
    serverLauncher.startListening();
  }

  @AfterEach
  void afterEach() {
    // closing the streams and pool should shut down the servers
    try {
      clientOut.close();
      serverOut.close();
    } catch (IOException e) {
      throw new IllegalStateException("Error in closing streams", e);
    }
    threadPool.shutdown();
  }

  @Test
  void testServer() {
    File root = Paths.get(
        System.getProperty("user.dir"),
        "..",
        "testProjects",
        "junit5-jupiter-starter-gradle").toFile();
    
    BuildClientCapabilities capabilities = new BuildClientCapabilities(Arrays.asList("java"));
    InitializeBuildParams params = new InitializeBuildParams(
        "test-client",
        "0.1.0",
        "0.1.0",
        root.toURI().toString(),
        capabilities
    );
    TestServer gradleBuildServer = clientLauncher.getRemoteProxy();

    // INITIALIZE
    gradleBuildServer.buildInitialize(params).join();
    gradleBuildServer.onBuildInitialized();

    // GET TARGETS
    WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
        .join();
    assertEquals(2, buildTargetsResult.getTargets().size());

    // CLEAN TARGETS
    List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
        .map(BuildTarget::getId)
        .collect(Collectors.toList());
    CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
    CleanCacheResult cleanResult = gradleBuildServer.buildTargetCleanCache(cleanCacheParams).join();
    // a clean will result in a single CompileReport because of GradleApiConnector#runTasks
    client.waitOnCompileReports(1);
    assertTrue(cleanResult.getCleaned());
    client.clearMessages();

    // COMPILE TARGETS
    CompileParams compileParams = new CompileParams(btIds);
    CompileResult compileResult = gradleBuildServer.buildTargetCompile(compileParams).join();
    assertEquals(StatusCode.OK, compileResult.getStatusCode());
    // compiling will result in a single CompileReport because of GradleApiConnector#runTasks
    client.waitOnCompileReports(1);
    client.clearMessages();

    // RETRIEVE TEST NAMES
    JvmTestEnvironmentParams testEnvParams = new JvmTestEnvironmentParams(btIds);
    JvmTestEnvironmentResult testEnvResult =
        gradleBuildServer.jvmTestEnvironment(testEnvParams).join();
    JvmEnvironmentItem testItem = findTest(testEnvResult, "com.example.project.CalculatorTests");

    // RUN TESTS
    List<String> mainClasses = testItem.getMainClasses().stream()
        .map(JvmMainClass::getClassName)
        .collect(Collectors.toList());
    ScalaTestClassesItem scalaTestClassesItem =
         new ScalaTestClassesItem(testItem.getTarget(), mainClasses);
    List<ScalaTestClassesItem> testClasses = new LinkedList<>();
    testClasses.add(scalaTestClassesItem);
    ScalaTestParams scalaTestParams = new ScalaTestParams();
    scalaTestParams.setTestClasses(testClasses);
    TestParams testParams = new TestParams(btIds);
    testParams.setDataKind(TestParamsDataKind.SCALA_TEST);
    testParams.setData(scalaTestParams);
    TestResult testResult = gradleBuildServer.buildTargetTest(testParams).join();
    client.waitOnTestFinishes(5);
    client.waitOnTestReports(1);
    assertEquals(StatusCode.OK, testResult.getStatusCode());
    client.clearMessages();

    // RUN MAIN
    ScalaMainClass mainClass = new ScalaMainClass("com.example.project.Calculator",
        Collections.emptyList(), Collections.emptyList());
    BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(),
        "junit5-jupiter-starter-gradle [main]");
    RunParams runParams = new RunParams(btId);
    runParams.setDataKind(RunParamsDataKind.SCALA_MAIN_CLASS);
    runParams.setData(mainClass);
    RunResult runResult = gradleBuildServer.buildTargetRun(runParams).join();
    client.waitOnGenericReports(1);
    assertEquals(StatusCode.OK, runResult.getStatusCode(),
            () -> client.genericReports.stream()
                    .collect(Collectors.joining("\n")));
    client.clearMessages();
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
}
