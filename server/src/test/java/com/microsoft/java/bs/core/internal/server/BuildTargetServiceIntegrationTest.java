package com.microsoft.java.bs.core.internal.server;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.CleanCacheParams;
import ch.epfl.scala.bsp4j.CleanCacheResult;
import ch.epfl.scala.bsp4j.CompileParams;
import ch.epfl.scala.bsp4j.CompileReport;
import ch.epfl.scala.bsp4j.CompileResult;
import ch.epfl.scala.bsp4j.DependencyModulesParams;
import ch.epfl.scala.bsp4j.DependencyModulesResult;
import ch.epfl.scala.bsp4j.DependencySourcesParams;
import ch.epfl.scala.bsp4j.DependencySourcesResult;
import ch.epfl.scala.bsp4j.LogMessageParams;
import ch.epfl.scala.bsp4j.MavenDependencyModule;
import ch.epfl.scala.bsp4j.MavenDependencyModuleArtifact;
import ch.epfl.scala.bsp4j.MessageType;
import ch.epfl.scala.bsp4j.ScalaTestClassesItem;
import ch.epfl.scala.bsp4j.ScalaTestParams;
import ch.epfl.scala.bsp4j.ScalaTestSuites;
import ch.epfl.scala.bsp4j.ScalaTestSuiteSelection;
import ch.epfl.scala.bsp4j.StatusCode;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TestParams;
import ch.epfl.scala.bsp4j.TestParamsDataKind;
import ch.epfl.scala.bsp4j.TestReport;
import ch.epfl.scala.bsp4j.TestResult;
import ch.epfl.scala.bsp4j.WorkspaceBuildTargetsResult;
import ch.epfl.scala.bsp4j.extended.TestFinishEx;
import ch.epfl.scala.bsp4j.extended.TestName;
import ch.epfl.scala.bsp4j.extended.TestStartEx;
import com.microsoft.java.bs.core.internal.utils.JsonUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BuildTargetServiceIntegrationTest extends IntegrationTest {

  private CompileReport findCompileReport(TestClient client, BuildTargetIdentifier btId) {
    CompileReport compileReport = client.compileReports.stream()
        .filter(report -> report.getTarget().equals(btId))
        .findFirst()
        .orElse(null);
    assertNotNull(compileReport, () -> {
      String availableTargets = client.compileReports.stream()
          .map(report -> report.getTarget().toString())
          .collect(Collectors.joining(", "));
      return "Target not found " + btId + ". Available: " + availableTargets;
    });
    return compileReport;
  }

  private List<String> getTestNameHierarchy(TestName testName) {
    List<String> names = new LinkedList<>();
    while (testName != null) {
      names.add(testName.getDisplayName());
      testName = testName.getParent();
    }
    return names;
  }

  private boolean matchesTest(TestName testName, String suiteName, String className,
                                     String methodName, List<String> testNames) {
    return Objects.equals(testName.getSuiteName(), suiteName)
        && Objects.equals(testName.getClassName(), className)
        && Objects.equals(testName.getMethodName(), methodName)
        && Objects.equals(getTestNameHierarchy(testName), testNames);
  }

  private String testNameAsString(TestName testName) {
    return testName.getSuiteName() + "," + testName.getClassName() + ","
        + testName.getMethodName() + "," + getTestNameHierarchy(testName);
  }

  TestStartEx getTestStart(TestClient client, String suiteName, String className, String methodName,
                           List<String> testNames) {
    return client.testStarts.stream().filter(ts -> matchesTest(ts.getTestName(),
            suiteName, className, methodName, testNames)).findAny()
        .orElseThrow(() -> new IllegalStateException("Missing test start for \n" + suiteName
            + "," + className + "," + methodName + "," + testNames + "\nonly found\n"
            + client.testStarts.stream().map(ts -> testNameAsString(ts.getTestName()))
            .collect(Collectors.joining("\n"))));
  }

  TestFinishEx getTestFinish(TestClient client, String suiteName, String className,
                             String methodName, List<String> testNames) {
    return client.testFinishes.stream().filter(ts -> matchesTest(ts.getTestName(),
            suiteName, className, methodName, testNames)).findAny()
        .orElseThrow(() -> new IllegalStateException("Missing test finish for\n" + suiteName
            + "," + className + "," + methodName + "," + testNames + "\nonly found\n"
            + client.testFinishes
            .stream().map(ts -> testNameAsString(ts.getTestName()))
            .collect(Collectors.joining("\n"))));
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
      client.waitOnCompileTasks(0);
      client.waitOnCompileReports(0);
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
      assertTrue(allArtifacts.stream()
          .anyMatch(artifact -> artifact.getUri().endsWith("-sources.jar")));

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
      assertEquals(StatusCode.OK, compileResult.getStatusCode());
      client.waitOnStartReports(2);
      client.waitOnFinishReports(2);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      for (BuildTargetIdentifier btId : btIds) {
        CompileReport compileReport = findCompileReport(client, btId);
        assertEquals("originId", compileReport.getOriginId());
        // TODO compile results are not yet implemented so always zero for now.
        assertEquals(0, compileReport.getWarnings());
        assertEquals(0, compileReport.getErrors());
      }
      client.clearMessages();
    });
  }

  @Test
  void testPassingJunit() {
    withNewTestServer("java-tests", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      gradleBuildServer.buildTargetCleanCache(cleanCacheParams).join();

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "java-tests [test]");

      // run passing tests
      List<String> passingTestMainClasses = new LinkedList<>();
      passingTestMainClasses.add("com.example.project.PassingTests");
      ScalaTestClassesItem passingTestClassesItem =
          new ScalaTestClassesItem(btId, passingTestMainClasses);
      List<ScalaTestClassesItem> passingTestClasses = new LinkedList<>();
      passingTestClasses.add(passingTestClassesItem);
      ScalaTestParams passingScalaTestParams = new ScalaTestParams();
      passingScalaTestParams.setTestClasses(passingTestClasses);
      TestParams passingTestParams = new TestParams(btIds);
      passingTestParams.setOriginId("originId");
      passingTestParams.setDataKind(TestParamsDataKind.SCALA_TEST);
      passingTestParams.setData(passingScalaTestParams);
      TestResult passingTestResult = gradleBuildServer.buildTargetTest(passingTestParams).join();
      assertEquals(StatusCode.OK, passingTestResult.getStatusCode());
      assertEquals("originId", passingTestResult.getOriginId());
      client.waitOnStartReports(10);
      client.waitOnFinishReports(11);
      client.waitOnCompileTasks(3);
      client.waitOnCompileReports(3);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(7);
      client.waitOnTestFinishes(7);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      TestReport passingTestsReport = client.testReports.get(0);
      assertEquals(5, passingTestsReport.getPassed());
      assertEquals(0, passingTestsReport.getCancelled());
      assertEquals(0, passingTestsReport.getFailed());
      assertEquals(0, passingTestsReport.getIgnored());
      assertEquals(0, passingTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.PassingTests",
          "com.example.project.PassingTests",
          null,
          List.of("PassingTests")));

      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.PassingTests",
          "isBasicTest()",
          List.of("isBasicTest()",
              "PassingTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.PassingTests",
          "hasDisplayName()",
          List.of("Display Name", "PassingTests")));
      assertNotNull(getTestStart(
          client,
          "isParameterized(int)",
          "com.example.project.PassingTests",
          "isParameterized(int)",
          List.of("isParameterized(int)",
              "PassingTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized(int)[1]",
          List.of("0",
              "isParameterized(int)",
              "PassingTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized(int)[2]",
          List.of("1",
              "isParameterized(int)",
              "PassingTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized(int)[3]",
          List.of("2",
              "isParameterized(int)",
              "PassingTests")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.PassingTests",
          "com.example.project.PassingTests",
          null,
          List.of("PassingTests")));

      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.PassingTests",
          "isBasicTest()",
          List.of("isBasicTest()",
              "PassingTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.PassingTests",
          "hasDisplayName()",
          List.of("Display Name", "PassingTests")));
      assertNotNull(getTestFinish(
          client,
          "isParameterized(int)",
          "com.example.project.PassingTests",
          "isParameterized(int)",
          List.of("isParameterized(int)",
              "PassingTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized(int)[1]",
          List.of("0",
              "isParameterized(int)",
              "PassingTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized(int)[2]",
          List.of("1",
              "isParameterized(int)",
              "PassingTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized(int)[3]",
          List.of("2",
              "isParameterized(int)",
              "PassingTests")));
      client.clearMessages();
    });
  }

  @Test
  void testFailingJunit() {
    withNewTestServer("java-tests", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      gradleBuildServer.buildTargetCleanCache(cleanCacheParams).join();

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "java-tests [test]");

      // run failing tests
      List<String> failingMainClasses = new LinkedList<>();
      failingMainClasses.add("com.example.project.FailingTests");
      ScalaTestClassesItem failingTestClassesItem =
          new ScalaTestClassesItem(btId, failingMainClasses);
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
      client.waitOnStartReports(4);
      client.waitOnFinishReports(5);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(2);
      client.waitOnTestFinishes(2);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      assertEquals(3, client.finishReportErrorCount());
      TestReport failingTestsReport = client.testReports.get(0);
      assertEquals(0, failingTestsReport.getPassed());
      assertEquals(0, failingTestsReport.getCancelled());
      assertEquals(1, failingTestsReport.getFailed());
      assertEquals(0, failingTestsReport.getIgnored());
      assertEquals(0, failingTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.FailingTests",
          "com.example.project.FailingTests",
          null,
          List.of("FailingTests")));

      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.FailingTests",
          "failingTest()",
          List.of("failingTest()", "FailingTests")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.FailingTests",
          "com.example.project.FailingTests",
          null,
          List.of("FailingTests")));
      TestFinishEx failingTestsFinish = getTestFinish(
          client,
          null,
          "com.example.project.FailingTests",
          "failingTest()",
          List.of("failingTest()", "FailingTests"));
      assertTrue(failingTestsFinish.getStackTrace()
          .contains("at com.example.project.FailingTests.failingTest(FailingTests.java:14)"));

      client.clearMessages();
    });
  }

  @Test
  void testStackTraceJunit() {
    withNewTestServer("java-tests", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      gradleBuildServer.buildTargetCleanCache(cleanCacheParams).join();

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "java-tests [test]");

      // run stacktrace test
      List<String> stacktraceMainClasses = new LinkedList<>();
      stacktraceMainClasses.add("com.example.project.ExceptionInBefore");
      ScalaTestClassesItem stacktraceTestClassesItem = new ScalaTestClassesItem(btId,
          stacktraceMainClasses);
      List<ScalaTestClassesItem> stacktraceTestClasses = new LinkedList<>();
      stacktraceTestClasses.add(stacktraceTestClassesItem);
      ScalaTestParams stacktraceScalaTestParams = new ScalaTestParams();
      stacktraceScalaTestParams.setTestClasses(stacktraceTestClasses);
      TestParams stacktraceTestParams = new TestParams(btIds);
      stacktraceTestParams.setOriginId("originId");
      stacktraceTestParams.setDataKind(TestParamsDataKind.SCALA_TEST);
      stacktraceTestParams.setData(stacktraceScalaTestParams);
      TestResult stacktraceTestResult = gradleBuildServer
          .buildTargetTest(stacktraceTestParams).join();
      assertEquals(StatusCode.ERROR, stacktraceTestResult.getStatusCode());
      assertEquals("originId", stacktraceTestResult.getOriginId());
      client.waitOnStartReports(4);
      client.waitOnFinishReports(5);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(2);
      client.waitOnTestFinishes(2);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      assertEquals(3, client.finishReportErrorCount());
      TestReport stacktraceTestsReport = client.testReports.get(0);
      assertEquals(0, stacktraceTestsReport.getPassed());
      assertEquals(0, stacktraceTestsReport.getCancelled());
      assertEquals(1, stacktraceTestsReport.getFailed());
      assertEquals(0, stacktraceTestsReport.getIgnored());
      assertEquals(0, stacktraceTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.ExceptionInBefore",
          "com.example.project.ExceptionInBefore",
          null,
          List.of("ExceptionInBefore")));

      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.ExceptionInBefore",
          "initializationError",
          List.of("initializationError",
              "ExceptionInBefore")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.ExceptionInBefore",
          "com.example.project.ExceptionInBefore",
          null,
          List.of("ExceptionInBefore")));
      TestFinishEx stacktraceTestsFinish = getTestFinish(
          client,
          null,
          "com.example.project.ExceptionInBefore",
          "initializationError",
          List.of("initializationError",
              "ExceptionInBefore"));
      assertTrue(stacktraceTestsFinish.getStackTrace().contains(
          "at com.example.project.ExceptionInBefore.beforeAll(ExceptionInBefore.java:13)"));

      client.clearMessages();
    });
  }

  @Test
  void testSingleMethodJunit() {
    withNewTestServer("java-tests", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      gradleBuildServer.buildTargetCleanCache(cleanCacheParams).join();

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "java-tests [test]");

      // run single method tests
      List<BuildTargetIdentifier> singleBt = new ArrayList<>();
      singleBt.add(btId);
      TestParams singleMethodTestParams = new TestParams(singleBt);
      singleMethodTestParams.setOriginId("originId");
      singleMethodTestParams.setDataKind("scala-test-suites-selection");
      List<String> singleTestMethods = new ArrayList<>();
      singleTestMethods.add("envVarSetTest");
      ScalaTestSuiteSelection singleScalaTestSuiteSelection = new ScalaTestSuiteSelection(
          "com.example.project.EnvVarTests", singleTestMethods);
      List<ScalaTestSuiteSelection> singleScalaTestSuiteSelections = new LinkedList<>();
      singleScalaTestSuiteSelections.add(singleScalaTestSuiteSelection);
      List<String> emptyJvmOptions = new ArrayList<>();
      List<String> environmentVariables = new ArrayList<>();
      environmentVariables.add("EnvVar=Test");
      ScalaTestSuites singleScalaTestSuites = new ScalaTestSuites(singleScalaTestSuiteSelections,
          emptyJvmOptions, environmentVariables);
      singleMethodTestParams.setData(singleScalaTestSuites);
      TestResult singleMethodTestResult =
          gradleBuildServer.buildTargetTest(singleMethodTestParams).join();
      assertEquals(StatusCode.OK, singleMethodTestResult.getStatusCode());
      assertEquals("originId", singleMethodTestResult.getOriginId());
      client.waitOnStartReports(5);
      client.waitOnFinishReports(6);
      client.waitOnCompileTasks(3);
      client.waitOnCompileReports(3);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(2);
      client.waitOnTestFinishes(2);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      TestReport singleMethodTestsReport = client.testReports.get(0);
      assertEquals(1, singleMethodTestsReport.getPassed());
      assertEquals(0, singleMethodTestsReport.getCancelled());
      assertEquals(0, singleMethodTestsReport.getFailed());
      assertEquals(0, singleMethodTestsReport.getIgnored());
      assertEquals(0, singleMethodTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.EnvVarTests",
          "com.example.project.EnvVarTests",
          null,
          List.of("EnvVarTests")));

      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.EnvVarTests",
          "envVarSetTest()",
          List.of("envVarSetTest()",
              "EnvVarTests")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.EnvVarTests",
          "com.example.project.EnvVarTests",
          null,
          List.of("EnvVarTests")));

      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.EnvVarTests",
          "envVarSetTest()",
          List.of("envVarSetTest()",
              "EnvVarTests")));
      client.clearMessages();
    });
  }

  @Test
  void testComplexHierarchyJunit() {
    withNewTestServer("java-tests", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      gradleBuildServer.buildTargetCleanCache(cleanCacheParams).join();

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "java-tests [test]");

      // run complex tests
      List<String> complexTestMainClasses = new LinkedList<>();
      complexTestMainClasses.add("com.example.project.TestFactoryTests");
      ScalaTestClassesItem complexTestClassesItem =
          new ScalaTestClassesItem(btId, complexTestMainClasses);
      List<ScalaTestClassesItem> complexTestClasses = new LinkedList<>();
      complexTestClasses.add(complexTestClassesItem);
      ScalaTestParams complexScalaTestParams = new ScalaTestParams();
      complexScalaTestParams.setTestClasses(complexTestClasses);
      TestParams complexTestParams = new TestParams(btIds);
      complexTestParams.setOriginId("originId");
      complexTestParams.setDataKind(TestParamsDataKind.SCALA_TEST);
      complexTestParams.setData(complexScalaTestParams);
      TestResult complexTestResult = gradleBuildServer.buildTargetTest(complexTestParams).join();
      assertEquals(StatusCode.OK, complexTestResult.getStatusCode());
      assertEquals("originId", complexTestResult.getOriginId());
      client.waitOnStartReports(11);
      client.waitOnFinishReports(12);
      client.waitOnCompileTasks(3);
      client.waitOnCompileReports(3);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(8);
      client.waitOnTestFinishes(8);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      TestReport complexTestsReport = client.testReports.get(0);
      assertEquals(4, complexTestsReport.getPassed());
      assertEquals(0, complexTestsReport.getCancelled());
      assertEquals(0, complexTestsReport.getFailed());
      assertEquals(0, complexTestsReport.getIgnored());
      assertEquals(0, complexTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.TestFactoryTests",
          "com.example.project.TestFactoryTests",
          null,
          List.of("TestFactoryTests")));
      assertNotNull(getTestStart(
          client,
          "testContainer()",
          "com.example.project.TestFactoryTests",
          "testContainer()",
          List.of("testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestStart(
          client,
          "testContainer()[1]",
          "com.example.project.TestFactoryTests",
          "testContainer()[1]",
          List.of("First Container",
              "testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.TestFactoryTests",
          "testContainer()[1][1]",
          List.of("First test of first container",
              "First Container",
              "testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.TestFactoryTests",
          "testContainer()[1][2]",
          List.of("Second test of first container",
              "First Container",
              "testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestStart(
          client,
          "testContainer()[2]",
          "com.example.project.TestFactoryTests",
          "testContainer()[2]",
          List.of("Second Container",
              "testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.TestFactoryTests",
          "testContainer()[2][1]",
          List.of("First test of second container",
              "Second Container",
              "testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.TestFactoryTests",
          "testContainer()[2][2]",
          List.of("Second test of second container",
              "Second Container",
              "testContainer()",
              "TestFactoryTests")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.TestFactoryTests",
          "com.example.project.TestFactoryTests",
          null,
          List.of("TestFactoryTests")));
      assertNotNull(getTestFinish(
          client,
          "testContainer()",
          "com.example.project.TestFactoryTests",
          "testContainer()",
          List.of("testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestFinish(
          client,
          "testContainer()[1]",
          "com.example.project.TestFactoryTests",
          "testContainer()[1]",
          List.of("First Container",
              "testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.TestFactoryTests",
          "testContainer()[1][1]",
          List.of("First test of first container",
              "First Container",
              "testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.TestFactoryTests",
          "testContainer()[1][2]",
          List.of("Second test of first container",
              "First Container",
              "testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestFinish(
          client,
          "testContainer()[2]",
          "com.example.project.TestFactoryTests",
          "testContainer()[2]",
          List.of("Second Container",
              "testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.TestFactoryTests",
          "testContainer()[2][1]",
          List.of("First test of second container",
              "Second Container",
              "testContainer()",
              "TestFactoryTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.TestFactoryTests",
          "testContainer()[2][2]",
          List.of("Second test of second container",
              "Second Container",
              "testContainer()",
              "TestFactoryTests")));
      client.clearMessages();
    });
  }

  @Test
  void testNestedJunit() {
    withNewTestServer("java-tests", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      gradleBuildServer.buildTargetCleanCache(cleanCacheParams).join();

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "java-tests [test]");

      // run nested tests
      List<String> nestedTestMainClasses = new LinkedList<>();
      nestedTestMainClasses.add("com.example.project.NestedTests");
      ScalaTestClassesItem nestedTestClassesItem =
          new ScalaTestClassesItem(btId, nestedTestMainClasses);
      List<ScalaTestClassesItem> nestedTestClasses = new LinkedList<>();
      nestedTestClasses.add(nestedTestClassesItem);
      ScalaTestParams nestedScalaTestParams = new ScalaTestParams();
      nestedScalaTestParams.setTestClasses(nestedTestClasses);
      TestParams nestedTestParams = new TestParams(btIds);
      nestedTestParams.setOriginId("originId");
      nestedTestParams.setDataKind(TestParamsDataKind.SCALA_TEST);
      nestedTestParams.setData(nestedScalaTestParams);
      TestResult nestedTestResult = gradleBuildServer.buildTargetTest(nestedTestParams).join();
      assertEquals(StatusCode.OK, nestedTestResult.getStatusCode());
      assertEquals("originId", nestedTestResult.getOriginId());
      client.waitOnStartReports(10);
      client.waitOnFinishReports(11);
      client.waitOnCompileTasks(3);
      client.waitOnCompileReports(3);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(7);
      client.waitOnTestFinishes(7);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      TestReport nestedTestsReport = client.testReports.get(0);
      assertEquals(3, nestedTestsReport.getPassed());
      assertEquals(0, nestedTestsReport.getCancelled());
      assertEquals(0, nestedTestsReport.getFailed());
      assertEquals(0, nestedTestsReport.getIgnored());
      assertEquals(0, nestedTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.NestedTests",
          "com.example.project.NestedTests",
          null,
          List.of("NestedTests")));
      assertNotNull(getTestStart(
          client,
          "com.example.project.NestedTests$NestedClassB",
          "com.example.project.NestedTests$NestedClassB",
          null,
          List.of("NestedClassB", "NestedTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.NestedTests$NestedClassB",
          "test()",
          List.of("test()",
              "NestedClassB",
              "NestedTests")));
      assertNotNull(getTestStart(
          client,
          "com.example.project.NestedTests$NestedClassB$ADeeperClass",
          "com.example.project.NestedTests$NestedClassB$ADeeperClass",
          null,
          List.of("ADeeperClass",
              "NestedClassB",
              "NestedTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.NestedTests$NestedClassB$ADeeperClass",
          "test()",
          List.of("test()",
              "ADeeperClass",
              "NestedClassB",
              "NestedTests")));
      assertNotNull(getTestStart(
          client,
          "com.example.project.NestedTests$NestedClassA",
          "com.example.project.NestedTests$NestedClassA",
          null,
          List.of("NestedClassA",
              "NestedTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.NestedTests$NestedClassA",
          "test()",
          List.of("test()",
              "NestedClassA",
              "NestedTests")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.NestedTests",
          "com.example.project.NestedTests",
          null,
          List.of("NestedTests")));
      assertNotNull(getTestFinish(
          client,
          "com.example.project.NestedTests$NestedClassB",
          "com.example.project.NestedTests$NestedClassB",
          null,
          List.of("NestedClassB", "NestedTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.NestedTests$NestedClassB",
          "test()",
          List.of("test()",
              "NestedClassB",
              "NestedTests")));
      assertNotNull(getTestFinish(
          client,
          "com.example.project.NestedTests$NestedClassB$ADeeperClass",
          "com.example.project.NestedTests$NestedClassB$ADeeperClass",
          null,
          List.of("ADeeperClass",
              "NestedClassB",
              "NestedTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.NestedTests$NestedClassB$ADeeperClass",
          "test()",
          List.of("test()",
              "ADeeperClass",
              "NestedClassB",
              "NestedTests")));
      assertNotNull(getTestFinish(
          client,
          "com.example.project.NestedTests$NestedClassA",
          "com.example.project.NestedTests$NestedClassA",
          null,
          List.of("NestedClassA",
              "NestedTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.NestedTests$NestedClassA",
          "test()",
          List.of("test()",
              "NestedClassA",
              "NestedTests")));
      client.clearMessages();
    });
  }

  @Test
  void testCleanStraightToJunit() {
    withNewTestServer("java-tests", (gradleBuildServer, client) -> {
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

      // a request to run tests straight after a clean should produce compile
      // results/reports
      // run tests
      List<String> mainClasses = new LinkedList<>();
      mainClasses.add("com.example.project.PassingTests");
      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "java-tests [test]");
      ScalaTestClassesItem scalaTestClassesItem = new ScalaTestClassesItem(btId, mainClasses);
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
      client.waitOnStartReports(10);
      client.waitOnFinishReports(11);
      client.waitOnCompileTasks(3);
      client.waitOnCompileReports(3);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(7);
      client.waitOnTestFinishes(7);
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
  void testFailingCompilation() {
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
      assertEquals(1, client.finishReportErrorCount());
      for (BuildTargetIdentifier btId : btIds) {
        CompileReport compileReport = findCompileReport(client, btId);
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

  @Test
  void testPassingTestNg() {
    withNewTestServer("testng", (gradleBuildServer, client) -> {
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

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "testng [test]");

      // run passing tests
      List<String> passingTestMainClasses = new LinkedList<>();
      passingTestMainClasses.add("com.example.project.PassingTests");
      ScalaTestClassesItem passingTestClassesItem =
          new ScalaTestClassesItem(btId, passingTestMainClasses);
      List<ScalaTestClassesItem> passingTestClasses = new LinkedList<>();
      passingTestClasses.add(passingTestClassesItem);
      ScalaTestParams passingScalaTestParams = new ScalaTestParams();
      passingScalaTestParams.setTestClasses(passingTestClasses);
      TestParams passingTestParams = new TestParams(btIds);
      passingTestParams.setOriginId("originId");
      passingTestParams.setDataKind(TestParamsDataKind.SCALA_TEST);
      passingTestParams.setData(passingScalaTestParams);
      TestResult passingTestResult = gradleBuildServer.buildTargetTest(passingTestParams).join();
      assertEquals(StatusCode.OK, passingTestResult.getStatusCode());
      assertEquals("originId", passingTestResult.getOriginId());
      client.waitOnStartReports(8);
      client.waitOnFinishReports(9);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(6);
      client.waitOnTestFinishes(6);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      TestReport passingTestsReport = client.testReports.get(0);
      assertEquals(5, passingTestsReport.getPassed());
      assertEquals(0, passingTestsReport.getCancelled());
      assertEquals(0, passingTestsReport.getFailed());
      assertEquals(0, passingTestsReport.getIgnored());
      assertEquals(0, passingTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.PassingTests",
          "com.example.project.PassingTests", null,
          List.of("com.example.project.PassingTests")));

      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.PassingTests",
          "isBasicTest",
          List.of("isBasicTest",
              "com.example.project.PassingTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.PassingTests",
          "hasDisplayName",
          List.of("hasDisplayName",
              "com.example.project.PassingTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized[0](0)",
          List.of("isParameterized[0](0)",
              "com.example.project.PassingTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized[1](1)",
          List.of("isParameterized[1](1)",
              "com.example.project.PassingTests")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized[2](2)",
          List.of("isParameterized[2](2)",
              "com.example.project.PassingTests")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.PassingTests",
          "com.example.project.PassingTests", null,
          List.of("com.example.project.PassingTests")));

      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.PassingTests",
          "isBasicTest",
          List.of("isBasicTest",
              "com.example.project.PassingTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.PassingTests",
          "hasDisplayName",
          List.of("hasDisplayName",
              "com.example.project.PassingTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized[0](0)",
          List.of("isParameterized[0](0)",
              "com.example.project.PassingTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized[1](1)",
          List.of("isParameterized[1](1)",
              "com.example.project.PassingTests")));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.PassingTests",
          "isParameterized[2](2)",
          List.of("isParameterized[2](2)",
              "com.example.project.PassingTests")));
      client.clearMessages();
    });
  }

  @Test
  void testFailingTestNg() {
    withNewTestServer("testng", (gradleBuildServer, client) -> {
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

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "testng [test]");

      // run failing tests
      List<String> failingMainClasses = new LinkedList<>();
      failingMainClasses.add("com.example.project.FailingTests");
      ScalaTestClassesItem failingTestClassesItem =
          new ScalaTestClassesItem(btId, failingMainClasses);
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
      client.waitOnStartReports(4);
      client.waitOnFinishReports(5);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(2);
      client.waitOnTestFinishes(2);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      assertEquals(3, client.finishReportErrorCount());
      TestReport failingTestsReport = client.testReports.get(0);
      assertEquals(0, failingTestsReport.getPassed());
      assertEquals(0, failingTestsReport.getCancelled());
      assertEquals(1, failingTestsReport.getFailed());
      assertEquals(0, failingTestsReport.getIgnored());
      assertEquals(0, failingTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.FailingTests",
          "com.example.project.FailingTests",
          null,
          List.of("com.example.project.FailingTests")));

      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.FailingTests",
          "failingTest",
          List.of("failingTest",
              "com.example.project.FailingTests")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.FailingTests",
          "com.example.project.FailingTests",
          null,
          List.of("com.example.project.FailingTests")));
      TestFinishEx failingTestsFinish = getTestFinish(
          client,
          null,
          "com.example.project.FailingTests",
          "failingTest",
          List.of("failingTest",
              "com.example.project.FailingTests"));
      assertTrue(failingTestsFinish.getStackTrace()
          .contains("at com.example.project.FailingTests.failingTest(FailingTests.java:13)"));

      client.clearMessages();
    });
  }

  @Test
  void testStackTraceTestNg() {
    withNewTestServer("testng", (gradleBuildServer, client) -> {
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

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "testng [test]");

      // run stacktrace test
      List<String> stacktraceMainClasses = new LinkedList<>();
      stacktraceMainClasses.add("com.example.project.ExceptionInBefore");
      ScalaTestClassesItem stacktraceTestClassesItem =
          new ScalaTestClassesItem(btId, stacktraceMainClasses);
      List<ScalaTestClassesItem> stacktraceTestClasses = new LinkedList<>();
      stacktraceTestClasses.add(stacktraceTestClassesItem);
      ScalaTestParams stacktraceScalaTestParams = new ScalaTestParams();
      stacktraceScalaTestParams.setTestClasses(stacktraceTestClasses);
      TestParams stacktraceTestParams = new TestParams(btIds);
      stacktraceTestParams.setOriginId("originId");
      stacktraceTestParams.setDataKind(TestParamsDataKind.SCALA_TEST);
      stacktraceTestParams.setData(stacktraceScalaTestParams);
      TestResult stacktraceTestResult = gradleBuildServer
          .buildTargetTest(stacktraceTestParams).join();
      assertEquals(StatusCode.ERROR, stacktraceTestResult.getStatusCode());
      assertEquals("originId", stacktraceTestResult.getOriginId());
      client.waitOnStartReports(5);
      client.waitOnFinishReports(6);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(3);
      client.waitOnTestFinishes(3);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      assertEquals(3, client.finishReportErrorCount());
      TestReport stacktraceTestsReport = client.testReports.get(0);
      assertEquals(0, stacktraceTestsReport.getPassed());
      assertEquals(0, stacktraceTestsReport.getCancelled());
      assertEquals(1, stacktraceTestsReport.getFailed());
      assertEquals(0, stacktraceTestsReport.getIgnored());
      assertEquals(1, stacktraceTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.ExceptionInBefore",
          "com.example.project.ExceptionInBefore",
          null,
          List.of("com.example.project.ExceptionInBefore")));

      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.ExceptionInBefore",
          "beforeAll",
          List.of("beforeAll",
              "com.example.project.ExceptionInBefore")));
      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.ExceptionInBefore",
          "test",
          List.of("test",
              "com.example.project.ExceptionInBefore")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.ExceptionInBefore",
          "com.example.project.ExceptionInBefore",
          null,
          List.of("com.example.project.ExceptionInBefore")));

      TestFinishEx stacktraceTestsFinish = getTestFinish(
          client,
          null,
          "com.example.project.ExceptionInBefore",
          "beforeAll",
          List.of("beforeAll",
              "com.example.project.ExceptionInBefore"));
      assertTrue(stacktraceTestsFinish.getStackTrace().contains(
          "at com.example.project.ExceptionInBefore.beforeAll(ExceptionInBefore.java:12)"));
      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.ExceptionInBefore",
          "test",
          List.of("test",
              "com.example.project.ExceptionInBefore")));

      client.clearMessages();
    });
  }

  @Test
  void testSingleMethodTestNg() {
    withNewTestServer("testng", (gradleBuildServer, client) -> {
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

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "testng [test]");

      // run single method tests
      List<BuildTargetIdentifier> singleBt = new ArrayList<>();
      singleBt.add(btId);
      TestParams singleMethodTestParams = new TestParams(singleBt);
      singleMethodTestParams.setOriginId("originId");
      singleMethodTestParams.setDataKind("scala-test-suites-selection");
      List<String> singleTestMethods = new ArrayList<>();
      singleTestMethods.add("envVarSetTest");
      ScalaTestSuiteSelection singleScalaTestSuiteSelection = new ScalaTestSuiteSelection(
          "com.example.project.EnvVarTests", singleTestMethods);
      List<ScalaTestSuiteSelection> singleScalaTestSuiteSelections = new LinkedList<>();
      singleScalaTestSuiteSelections.add(singleScalaTestSuiteSelection);
      List<String> emptyJvmOptions = new ArrayList<>();
      List<String> environmentVariables = new ArrayList<>();
      environmentVariables.add("EnvVar=Test");
      ScalaTestSuites singleScalaTestSuites = new ScalaTestSuites(singleScalaTestSuiteSelections,
          emptyJvmOptions, environmentVariables);
      singleMethodTestParams.setData(singleScalaTestSuites);
      TestResult singleMethodTestResult =
          gradleBuildServer.buildTargetTest(singleMethodTestParams).join();
      assertEquals(StatusCode.OK, singleMethodTestResult.getStatusCode());
      assertEquals("originId", singleMethodTestResult.getOriginId());
      client.waitOnStartReports(4);
      client.waitOnFinishReports(5);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(2);
      client.waitOnTestFinishes(2);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      TestReport singleMethodTestsReport = client.testReports.get(0);
      assertEquals(1, singleMethodTestsReport.getPassed());
      assertEquals(0, singleMethodTestsReport.getCancelled());
      assertEquals(0, singleMethodTestsReport.getFailed());
      assertEquals(0, singleMethodTestsReport.getIgnored());
      assertEquals(0, singleMethodTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.EnvVarTests",
          "com.example.project.EnvVarTests",
          null,
          List.of("com.example.project.EnvVarTests")));

      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.EnvVarTests",
          "envVarSetTest",
          List.of("envVarSetTest",
              "com.example.project.EnvVarTests")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.EnvVarTests",
          "com.example.project.EnvVarTests",
          null,
          List.of("com.example.project.EnvVarTests")));

      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.EnvVarTests",
          "envVarSetTest",
          List.of("envVarSetTest",
              "com.example.project.EnvVarTests")));
      client.clearMessages();
    });
  }

  @Test
  void testSpock() {
    withNewTestServer("spock", (gradleBuildServer, client) -> {
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

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      // run tests
      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(), "spock [test]");
      List<String> passingTestMainClasses = new LinkedList<>();
      passingTestMainClasses.add("com.example.project.SpockTest");
      ScalaTestClassesItem passingTestClassesItem =
          new ScalaTestClassesItem(btId, passingTestMainClasses);
      List<ScalaTestClassesItem> passingTestClasses = new LinkedList<>();
      passingTestClasses.add(passingTestClassesItem);
      ScalaTestParams passingScalaTestParams = new ScalaTestParams();
      passingScalaTestParams.setTestClasses(passingTestClasses);
      TestParams passingTestParams = new TestParams(btIds);
      passingTestParams.setOriginId("originId");
      passingTestParams.setDataKind(TestParamsDataKind.SCALA_TEST);
      passingTestParams.setData(passingScalaTestParams);
      TestResult passingTestResult = gradleBuildServer.buildTargetTest(passingTestParams).join();
      // Caveat - this may fail if using too high JDK version for SPOCK.
      // Dependency `org.spockframework:spock-core:2.3-groovy-4.0` may need upgrading.
      assertEquals(StatusCode.OK, passingTestResult.getStatusCode());
      assertEquals("originId", passingTestResult.getOriginId());
      client.waitOnStartReports(4);
      client.waitOnFinishReports(5);
      client.waitOnCompileTasks(2);
      client.waitOnCompileReports(2);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(2);
      client.waitOnTestFinishes(2);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      TestReport passingTestsReport = client.testReports.get(0);
      assertEquals(1, passingTestsReport.getPassed());
      assertEquals(0, passingTestsReport.getCancelled());
      assertEquals(0, passingTestsReport.getFailed());
      assertEquals(0, passingTestsReport.getIgnored());
      assertEquals(0, passingTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.SpockTest",
          "com.example.project.SpockTest",
          null,
          List.of("SpockTest")));

      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.SpockTest",
          "zero is zero",
          List.of("zero is zero",
              "SpockTest")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.SpockTest",
          "com.example.project.SpockTest",
          null,
          List.of("SpockTest")));

      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.SpockTest",
          "zero is zero",
          List.of("zero is zero",
              "SpockTest")));
      client.clearMessages();
    });
  }

  @Test
  void testExtraConfiguration() {
    withNewTestServer("java-tests", (gradleBuildServer, client) -> {
      // get targets
      WorkspaceBuildTargetsResult buildTargetsResult = gradleBuildServer.workspaceBuildTargets()
          .join();
      List<BuildTargetIdentifier> btIds = buildTargetsResult.getTargets().stream()
          .map(BuildTarget::getId)
          .collect(Collectors.toList());

      // clean targets
      CleanCacheParams cleanCacheParams = new CleanCacheParams(btIds);
      gradleBuildServer.buildTargetCleanCache(cleanCacheParams).join();

      // compile targets
      CompileParams compileParams = new CompileParams(btIds);
      compileParams.setOriginId("originId");
      gradleBuildServer.buildTargetCompile(compileParams).join();
      client.clearMessages();

      BuildTargetIdentifier btId = findTarget(buildTargetsResult.getTargets(),
          "java-tests [extraTest]");

      // run single method tests
      List<BuildTargetIdentifier> singleBt = new ArrayList<>();
      singleBt.add(btId);

      List<String> passingTestMainClasses = new LinkedList<>();
      passingTestMainClasses.add("com.example.project.ExtraTests");
      ScalaTestClassesItem passingTestClassesItem =
          new ScalaTestClassesItem(btId, passingTestMainClasses);
      List<ScalaTestClassesItem> passingTestClasses = new LinkedList<>();
      passingTestClasses.add(passingTestClassesItem);
      ScalaTestParams passingScalaTestParams = new ScalaTestParams();
      passingScalaTestParams.setTestClasses(passingTestClasses);
      TestParams passingTestParams = new TestParams(btIds);
      passingTestParams.setOriginId("originId");
      passingTestParams.setDataKind(TestParamsDataKind.SCALA_TEST);
      passingTestParams.setData(passingScalaTestParams);
      TestResult passingTestResult = gradleBuildServer.buildTargetTest(passingTestParams).join();
      assertEquals(StatusCode.OK, passingTestResult.getStatusCode());
      assertEquals("originId", passingTestResult.getOriginId());
      client.waitOnStartReports(5);
      client.waitOnFinishReports(6);
      client.waitOnCompileTasks(3);
      client.waitOnCompileReports(3);
      client.waitOnLogMessages(0);
      client.waitOnTestStarts(2);
      client.waitOnTestFinishes(2);
      client.waitOnTestReports(1);
      for (CompileReport message : client.compileReports) {
        assertTrue(message.getNoOp());
      }
      for (TaskFinishParams message : client.finishReports) {
        assertEquals(StatusCode.OK, message.getStatus());
      }
      TestReport singleMethodTestsReport = client.testReports.get(0);
      assertEquals(1, singleMethodTestsReport.getPassed());
      assertEquals(0, singleMethodTestsReport.getCancelled());
      assertEquals(0, singleMethodTestsReport.getFailed());
      assertEquals(0, singleMethodTestsReport.getIgnored());
      assertEquals(0, singleMethodTestsReport.getSkipped());

      assertNotNull(getTestStart(
          client,
          "com.example.project.ExtraTests",
          "com.example.project.ExtraTests",
          null,
          List.of("ExtraTests")));

      assertNotNull(getTestStart(
          client,
          null,
          "com.example.project.ExtraTests",
          "extraTest()",
          List.of("extraTest()",
              "ExtraTests")));

      assertNotNull(getTestFinish(
          client,
          "com.example.project.ExtraTests",
          "com.example.project.ExtraTests",
          null,
          List.of("ExtraTests")));

      assertNotNull(getTestFinish(
          client,
          null,
          "com.example.project.ExtraTests",
          "extraTest()",
          List.of("extraTest()",
              "ExtraTests")));
      client.clearMessages();
    });
  }

}
