// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import com.microsoft.java.bs.gradle.model.ScalaExtension;
import com.microsoft.java.bs.gradle.model.SupportedLanguages;
import com.microsoft.java.bs.gradle.model.utils.Conversions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.microsoft.java.bs.core.Launcher;
import com.microsoft.java.bs.core.internal.managers.PreferenceManager;
import com.microsoft.java.bs.core.internal.model.Preferences;
import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

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

  private GradleApiConnector getConnector() {
    List<String> supportedLanguages = new LinkedList<>();
    supportedLanguages.add(SupportedLanguages.JAVA);
    supportedLanguages.add(SupportedLanguages.SCALA);
    PreferenceManager preferenceManager = new PreferenceManager();
    preferenceManager.setClientSupportedLanguages(supportedLanguages);
    preferenceManager.setPreferences(new Preferences());
    return new GradleApiConnector(preferenceManager);
  }

  @Test
  void testGetGradleVersion() {
    File projectDir = projectPath.resolve("gradle-4.3-with-wrapper").toFile();
    GradleApiConnector connector = getConnector();
    assertEquals("4.3", connector.getGradleVersion(projectDir.toURI()));
  }

  @Test
  void testGetGradleSourceSets() {
    File projectDir = projectPath.resolve("junit5-jupiter-starter-gradle").toFile();
    GradleApiConnector connector = getConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
    for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
      assertNotNull(gradleSourceSet.getExtensions().get(SupportedLanguages.JAVA));
      assertNull(gradleSourceSet.getExtensions().get(SupportedLanguages.SCALA));
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

  @Test
  void testGetGradleDuplicateNestedProjectNames() {
    File projectDir = projectPath.resolve("duplicate-nested-project-names").toFile();
    GradleApiConnector connector = getConnector();
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
    GradleApiConnector connector = getConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(5, gradleSourceSets.getGradleSourceSets().size());
    assertFalse(findSourceSet(gradleSourceSets, "test-tag [main]").hasTests());
    assertTrue(findSourceSet(gradleSourceSets, "test-tag [test]").hasTests());
    assertFalse(findSourceSet(gradleSourceSets, "test-tag [noTests]").hasTests());
    assertTrue(findSourceSet(gradleSourceSets, "test-tag [intTest]").hasTests());
    assertFalse(findSourceSet(gradleSourceSets, "test-tag [testFixtures]").hasTests());
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
    GradleApiConnector connector = getConnector();
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
    GradleApiConnector connector = getConnector();
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
    GradleApiConnector connector = getConnector();
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
    GradleApiConnector connector = getConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(4, gradleSourceSets.getGradleSourceSets().size());
    GradleSourceSet mainA = findSourceSet(gradleSourceSets, "a [main]");
    GradleSourceSet mainB = findSourceSet(gradleSourceSets, "b [main]");
    assertHasBuildTargetDependency(mainB, mainA);
  }

  @Test
  void testGetGradleDependenciesWithTestConfiguration() {
    File projectDir = projectPath.resolve("project-dependency-test-configuration").toFile();
    GradleApiConnector connector = getConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(4, gradleSourceSets.getGradleSourceSets().size());
    GradleSourceSet testA = findSourceSet(gradleSourceSets, "a [test]");
    GradleSourceSet testB = findSourceSet(gradleSourceSets, "b [test]");
    assertHasBuildTargetDependency(testB, testA);
  }

  @Test
  void testGetGradleDependenciesWithLazyArchive() {
    File projectDir = projectPath.resolve("project-dependency-lazy-archive").toFile();
    GradleApiConnector connector = getConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(4, gradleSourceSets.getGradleSourceSets().size());
    GradleSourceSet testA = findSourceSet(gradleSourceSets, "a [test]");
    GradleSourceSet testB = findSourceSet(gradleSourceSets, "b [test]");
    assertHasBuildTargetDependency(testB, testA);
  }

  @Test
  void testGetGradleHasScala2() {
    File projectDir = projectPath.resolve("scala-2").toFile();
    GradleApiConnector connector = getConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
    GradleSourceSet main = findSourceSet(gradleSourceSets, "scala-2 [main]");
    ScalaExtension scalaExtension = Conversions.toScalaExtension(
        main.getExtensions().get(SupportedLanguages.SCALA));
    assertNotNull(scalaExtension);
    assertEquals("org.scala-lang", scalaExtension.getScalaOrganization());
    assertEquals("2.13.12", scalaExtension.getScalaVersion());
    assertEquals("2.13", scalaExtension.getScalaBinaryVersion());

    assertFalse(main.getCompileClasspath().isEmpty());
    assertTrue(main.getCompileClasspath().stream().anyMatch(
            file -> file.getName().contains("scala-library")));
    assertFalse(scalaExtension.getScalaJars().isEmpty());
    assertTrue(scalaExtension.getScalaJars().stream().anyMatch(
            file -> file.getName().contains("scala-compiler")));
    assertFalse(scalaExtension.getScalaCompilerArgs().isEmpty());
    assertTrue(scalaExtension.getScalaCompilerArgs().stream()
        .anyMatch(arg -> arg.equals("-deprecation")));
  }

  @Test
  void testGetGradleHasScala3() {
    File projectDir = projectPath.resolve("scala-3").toFile();
    GradleApiConnector connector = getConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
    assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
    GradleSourceSet main = findSourceSet(gradleSourceSets, "scala-3 [main]");
    ScalaExtension scalaExtension = Conversions.toScalaExtension(
        main.getExtensions().get(SupportedLanguages.SCALA));
    assertNotNull(scalaExtension);
    assertEquals("org.scala-lang", scalaExtension.getScalaOrganization());
    assertEquals("3.3.1", scalaExtension.getScalaVersion());
    assertEquals("3.3", scalaExtension.getScalaBinaryVersion());

    assertFalse(main.getCompileClasspath().isEmpty());
    assertTrue(main.getCompileClasspath().stream().anyMatch(
            file -> file.getName().contains("scala3-library_3")));
    assertFalse(scalaExtension.getScalaJars().isEmpty());
    assertTrue(scalaExtension.getScalaJars().stream().anyMatch(
            file -> file.getName().contains("scala3-compiler_3")));
    assertFalse(scalaExtension.getScalaCompilerArgs().isEmpty());
    assertTrue(scalaExtension.getScalaCompilerArgs().stream()
        .anyMatch(arg -> arg.equals("-deprecation")));
  }
}
