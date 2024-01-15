// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import ch.epfl.scala.bsp4j.BuildClient;
import ch.epfl.scala.bsp4j.DidChangeBuildTarget;
import ch.epfl.scala.bsp4j.LogMessageParams;
import ch.epfl.scala.bsp4j.PublishDiagnosticsParams;
import ch.epfl.scala.bsp4j.ShowMessageParams;
import ch.epfl.scala.bsp4j.TaskFinishParams;
import ch.epfl.scala.bsp4j.TaskProgressParams;
import ch.epfl.scala.bsp4j.TaskStartParams;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.Launcher;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.GradleTestEntity;
import com.microsoft.java.bs.core.internal.model.Preferences;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.GradleTestTask;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;

class GradleApiConnectorTest {

  private static Path projectPath;

  @BeforeAll
  static void beforeClass() {
    projectPath = Paths.get(
        System.getProperty("user.dir"),
        "..",
        "testProjects"
    ).normalize();
    String pluginDir = Paths.get(System.getProperty("user.dir"),
        "build", "libs", "plugins").toString();
    System.setProperty(Launcher.PROP_PLUGIN_DIR, pluginDir);
  }
  
  private static class TestClient implements BuildClient {

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
      // do nothing
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

  private GradleApiConnector getApiConnector(Preferences preferences) {
    PreferenceManager preferenceManager = new PreferenceManager();
    preferenceManager.setPreferences(preferences);
    GradleApiConnector connector = new GradleApiConnector(preferenceManager);
    connector.setClient(new TestClient());
    return connector;
  }

  private GradleApiConnector getApiConnector() {
    return getApiConnector(new Preferences());
  }

  @Test
  void testGetGradleVersion() {
    File projectDir = projectPath.resolve("gradle-4.3-with-wrapper").toFile();
    GradleApiConnector connector = getApiConnector();
    assertEquals("4.3", connector.getGradleVersion(projectDir.toURI()));
  }

  @Test
  void testGetGradleSourceSets() {
    File projectDir = projectPath.resolve("junit5-jupiter-starter-gradle").toFile();
    GradleApiConnector connector = getApiConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
    for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
      assertEquals("junit5-jupiter-starter-gradle", gradleSourceSet.getProjectName());
      assertEquals(":", gradleSourceSet.getProjectPath());
      assertEquals(projectDir, gradleSourceSet.getProjectDir());
      assertEquals(projectDir, gradleSourceSet.getRootDir());
    }
    
    assertEquals("main", findSourceSet(gradleSourceSets,
        "junit5-jupiter-starter-gradle [main]").getSourceSetName());
    assertEquals("test", findSourceSet(gradleSourceSets,
        "junit5-jupiter-starter-gradle [test]").getSourceSetName());
  }

  private GradleSourceSet findSourceSet(GradleSourceSets gradleSourceSets, String displayName) {
    GradleSourceSet sourceSet = gradleSourceSets.getGradleSourceSets().stream()
        .filter(ss -> ss.getDisplayName().equals(displayName))
        .findFirst()
        .orElse(null);
    assertNotNull(sourceSet, () -> {
      String availableSourceSets = gradleSourceSets.getGradleSourceSets().stream()
          .map(ss -> ss.getDisplayName())
          .collect(Collectors.joining(", "));
      return "DisplayName not found " + displayName + ". Available: " + availableSourceSets;
    });
    return sourceSet;
  }

  private void assertHasTaskPath(Set<GradleTestTask> paths, String path) {
    assertTrue(paths.stream().anyMatch(task -> task.getTaskPath().equals(path)), () -> {
      String pathsAsStr = paths.stream().map(task -> task.getTaskPath())
          .collect(Collectors.joining(", "));
      return "Task path not found [" + path + "] in [" + pathsAsStr + ']';
    });
  }

  @Test
  void testGetGradleDuplicateNestedProjectNames() {
    File projectDir = projectPath.resolve("duplicate-nested-project-names").toFile();
    GradleApiConnector connector = getApiConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(12, gradleSourceSets.getGradleSourceSets().size());
    findSourceSet(gradleSourceSets, "a [main]");
    findSourceSet(gradleSourceSets, "a [test]");
    findSourceSet(gradleSourceSets, "b [main]");
    findSourceSet(gradleSourceSets, "b [test]");
    findSourceSet(gradleSourceSets, "b-test [main]");
    findSourceSet(gradleSourceSets, "b-test [test]");
    findSourceSet(gradleSourceSets, "c [main]");
    findSourceSet(gradleSourceSets, "c [test]");
    findSourceSet(gradleSourceSets, "d [main]");
    findSourceSet(gradleSourceSets, "d [test]");
    findSourceSet(gradleSourceSets, "e [main]");
    findSourceSet(gradleSourceSets, "e [test]");
  }

  @Test
  void testGetGradleHasTests() {
    File projectDir = projectPath.resolve("test-tag").toFile();
    GradleApiConnector connector = getApiConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(5, gradleSourceSets.getGradleSourceSets().size());

    GradleSourceSet main = findSourceSet(gradleSourceSets, "test-tag [main]");
    assertFalse(main.hasTests());
    GradleSourceSet test = findSourceSet(gradleSourceSets, "test-tag [test]");
    assertTrue(test.hasTests());
    assertEquals(1, test.getTestTasks().size());
    assertHasTaskPath(test.getTestTasks(), ":test");
    GradleSourceSet noTests = findSourceSet(gradleSourceSets, "test-tag [noTests]");
    assertFalse(noTests.hasTests());
    GradleSourceSet intTest = findSourceSet(gradleSourceSets, "test-tag [intTest]");
    assertTrue(intTest.hasTests());
    assertEquals(1, intTest.getTestTasks().size());
    assertHasTaskPath(intTest.getTestTasks(), ":integrationTest");
    GradleSourceSet testFixtures = findSourceSet(gradleSourceSets, "test-tag [testFixtures]");
    assertFalse(testFixtures.hasTests());
  }

  private void assertHasBuildTargetDependency(GradleSourceSet sourceSet,
      GradleSourceSet dependency) {
    boolean exists = sourceSet.getBuildTargetDependencies().stream()
        .anyMatch(dep -> dep.getProjectPath().equals(dependency.getProjectPath())
                      && dep.getSourceSetName().equals(dependency.getSourceSetName()));
    assertTrue(exists, () -> {
      String availableDependencies = sourceSet.getBuildTargetDependencies().stream()
          .map(ss -> ss.getProjectPath() + ' ' + ss.getSourceSetName())
          .collect(Collectors.joining(", "));
      return "Dependency not found " + dependency.getProjectPath() + ' '
        + dependency.getSourceSetName() + ". Available: " + availableDependencies;
    });
  }

  @Test
  void testGetGradleDependenciesWithTestFixtures() {
    File projectDir = projectPath.resolve("project-dependency-test-fixtures").toFile();
    GradleApiConnector connector = getApiConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(5, gradleSourceSets.getGradleSourceSets().size());
    GradleSourceSet mainA = findSourceSet(gradleSourceSets, "a [main]");
    assertEquals(0, mainA.getBuildTargetDependencies().size());
    GradleSourceSet testFixturesA = findSourceSet(gradleSourceSets, "a [testFixtures]");
    assertEquals(1, testFixturesA.getBuildTargetDependencies().size());
    GradleSourceSet testA = findSourceSet(gradleSourceSets, "a [test]");
    assertEquals(2, testA.getBuildTargetDependencies().size());
    GradleSourceSet mainB = findSourceSet(gradleSourceSets, "b [main]");
    assertEquals(0, mainB.getBuildTargetDependencies().size());
    GradleSourceSet testB = findSourceSet(gradleSourceSets, "b [test]");
    assertEquals(3, testB.getBuildTargetDependencies().size());
    assertHasBuildTargetDependency(testFixturesA, mainA);
    assertHasBuildTargetDependency(testA, mainA);
    assertHasBuildTargetDependency(testA, testFixturesA);
    assertHasBuildTargetDependency(testB, testFixturesA);
    assertHasBuildTargetDependency(testB, mainA);
    assertHasBuildTargetDependency(testB, mainB);
  }

  @Test
  void testGetGradleDependenciesWithTestToMain() {
    File projectDir = projectPath.resolve("project-dependency-test-to-main").toFile();
    GradleApiConnector connector = getApiConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
    GradleSourceSet main = findSourceSet(gradleSourceSets,
        "project-dependency-test-to-main [main]");
    GradleSourceSet test = findSourceSet(gradleSourceSets,
        "project-dependency-test-to-main [test]");
    assertEquals(0, main.getBuildTargetDependencies().size());
    assertHasBuildTargetDependency(test, main);
    assertEquals(1, test.getBuildTargetDependencies().size());
  }

  @Test
  void testGetGradleDependenciesWithSourceSetOutput() {
    File projectDir = projectPath.resolve("project-dependency-sourceset-output").toFile();
    GradleApiConnector connector = getApiConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(4, gradleSourceSets.getGradleSourceSets().size());
    GradleSourceSet testA = findSourceSet(gradleSourceSets, "a [test]");
    assertEquals(1, testA.getBuildTargetDependencies().size());
    GradleSourceSet mainB = findSourceSet(gradleSourceSets, "b [main]");
    assertEquals(0, mainB.getBuildTargetDependencies().size());
    GradleSourceSet testB = findSourceSet(gradleSourceSets, "b [test]");
    assertEquals(2, testB.getBuildTargetDependencies().size());
    assertHasBuildTargetDependency(testB, testA);
    assertHasBuildTargetDependency(testB, mainB);
  }

  @Test
  void testGetGradleDependenciesWithConfiguration() {
    File projectDir = projectPath.resolve("project-dependency-configuration").toFile();
    GradleApiConnector connector = getApiConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(4, gradleSourceSets.getGradleSourceSets().size());
    GradleSourceSet mainA = findSourceSet(gradleSourceSets, "a [main]");
    GradleSourceSet mainB = findSourceSet(gradleSourceSets, "b [main]");
    assertHasBuildTargetDependency(mainB, mainA);
  }

  @Test
  void testGetGradleDependenciesWithTestConfiguration() {
    File projectDir = projectPath.resolve("project-dependency-test-configuration").toFile();
    GradleApiConnector connector = getApiConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(4, gradleSourceSets.getGradleSourceSets().size());
    GradleSourceSet testA = findSourceSet(gradleSourceSets, "a [test]");
    GradleSourceSet testB = findSourceSet(gradleSourceSets, "b [test]");
    assertHasBuildTargetDependency(testB, testA);
  }

  @Test
  void testGetGradleDependenciesWithLazyArchive() {
    File projectDir = projectPath.resolve("project-dependency-lazy-archive").toFile();
    GradleApiConnector connector = getApiConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(4, gradleSourceSets.getGradleSourceSets().size());
    GradleSourceSet testA = findSourceSet(gradleSourceSets, "a [test]");
    GradleSourceSet testB = findSourceSet(gradleSourceSets, "b [test]");
    assertHasBuildTargetDependency(testB, testA);
  }

  @Test
  void testGetJvmTestEnviroment() {
    File projectDir = projectPath.resolve("junit5-jupiter-starter-gradle").toFile();
    Preferences preferences = new Preferences();
    // test discovery uses --test-dry-run which was added in 8.3
    preferences.setGradleVersion("8.3");
    GradleApiConnector connector = getApiConnector(preferences);
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());

    Map<BuildTargetIdentifier, Set<GradleTestTask>> testTaskMap = new HashMap<>();
    for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
      BuildTargetIdentifier fakeBt = new BuildTargetIdentifier(gradleSourceSet.getDisplayName());
      testTaskMap.put(fakeBt, gradleSourceSet.getTestTasks());
    }
    Map<BuildTargetIdentifier, List<GradleTestEntity>> tests =
          connector.getTestClasses(projectDir.toURI(), testTaskMap);
    assertHasTestClass(tests, "junit5-jupiter-starter-gradle [test]",
        "com.example.project.CalculatorTests");
  }

  private void assertHasTestClass(Map<BuildTargetIdentifier, List<GradleTestEntity>> tests,
      String sourceSetDisplayName, String className) {
    List<GradleTestEntity> btTests = tests.entrySet().stream()
        .filter(entry -> entry.getKey().getUri().equals(sourceSetDisplayName))
        .flatMap(entry -> entry.getValue().stream())
        .collect(Collectors.toList());
    assertTrue(btTests.size() > 0,
        () -> "SourceSet " + sourceSetDisplayName + " not found in "
        + tests.keySet().stream()
          .map(bt -> bt.getUri())
          .collect(Collectors.joining(", ")));

    List<String> btTestClasses = btTests.stream()
        .flatMap(entity -> entity.getTestClasses().stream())
        .collect(Collectors.toList());
    assertTrue(btTestClasses.contains(className),
        () -> "Test class " + className + " not found in "
        + String.join(", ", btTestClasses));
  }

  @Test
  void testBuildTargetTest() {
    File projectDir = projectPath.resolve("java-tests").toFile();
    GradleApiConnector connector = getApiConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    GradleSourceSet testSourceSet =
        findSourceSet(gradleSourceSets, "java-tests [test]");
    Map<BuildTargetIdentifier, Set<String>> testClassesMap = new HashMap<>();
    BuildTargetIdentifier fakeBt = new BuildTargetIdentifier(testSourceSet.getDisplayName());
    Set<String> classes = new HashSet<>();
    testClassesMap.put(fakeBt, classes);
    classes.add("com.example.project.PassingTests");
    StatusCode passingTest = connector.runTestClasses(projectDir.toURI(), testClassesMap);
    assertEquals(StatusCode.OK, passingTest);
    classes.clear();
    classes.add("com.example.project.FailingTests");
    StatusCode failingTest = connector.runTestClasses(projectDir.toURI(), testClassesMap);
    assertEquals(StatusCode.ERROR, failingTest);   
  }
}
