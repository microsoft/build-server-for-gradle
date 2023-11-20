// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;

class GradleBuildServerPluginTest {

  private static Path projectPath;

  @BeforeAll
  static void beforeClass() {
    projectPath = Paths.get(
        System.getProperty("user.dir"),
        "..",
        "testProjects"
    ).normalize();
  }

  @Test
  void testModelBuilder() throws IOException {
    File projectDir = projectPath.resolve("junit5-jupiter-starter-gradle").toFile();
    GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
    connector.useBuildDistribution();
    try (ProjectConnection connect = connector.connect()) {
      ModelBuilder<GradleSourceSets> modelBuilder = connect.model(GradleSourceSets.class);
      File initScript = PluginHelper.getInitScript();
      modelBuilder.addArguments("--init-script", initScript.getAbsolutePath());
      GradleSourceSets gradleSourceSets = modelBuilder.get();
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        assertEquals("junit5-jupiter-starter-gradle", gradleSourceSet.getProjectName());
        assertEquals(":", gradleSourceSet.getProjectPath());
        assertEquals(projectDir, gradleSourceSet.getProjectDir());
        assertEquals(projectDir, gradleSourceSet.getRootDir());
        assertTrue(gradleSourceSet.getSourceSetName().equals("main")
            || gradleSourceSet.getSourceSetName().equals("test"));
        assertTrue(gradleSourceSet.getClassesTaskName().equals("classes")
            || gradleSourceSet.getClassesTaskName().equals("testClasses"));
        assertTrue(gradleSourceSet.getSourceDirs().size() > 0);
        assertTrue(gradleSourceSet.getGeneratedSourceDirs().size() > 0);
        assertTrue(gradleSourceSet.getResourceDirs().size() > 0);
        assertTrue(gradleSourceSet.getCompileClasspath().size() > 0);
        assertNotNull(gradleSourceSet.getSourceOutputDir());
        assertNotNull(gradleSourceSet.getResourceOutputDir());

        assertNotNull(gradleSourceSet.getJavaHome());
        assertNotNull(gradleSourceSet.getJavaVersion());
        assertNotNull(gradleSourceSet.getSourceCompatibility());
        assertNotNull(gradleSourceSet.getTargetCompatibility());
        assertNotNull(gradleSourceSet.getCompilerArgs());
        assertNotNull(gradleSourceSet.getGradleVersion());
        assertNotNull(gradleSourceSet.getProjectDependencies());
        assertNotNull(gradleSourceSet.getModuleDependencies());
        assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
            dependency -> dependency.getModule().equals("a.jar")
        ));
        assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
            dependency -> dependency.getModule().contains("gradle-api")
        ));
      }
    }
  }

  @Test
  @EnabledOnJre({JRE.JAVA_8})
  void testGetSourceContainerFromOldGradle() throws IOException {
    File projectDir = projectPath.resolve("non-java").toFile();
    GradleConnector connector = GradleConnector.newConnector()
        .forProjectDirectory(projectDir)
        .useGradleVersion("4.8");
    try (ProjectConnection connect = connector.connect()) {
      File initScript = PluginHelper.getInitScript();
      GradleSourceSets gradleSourceSets = connect.model(GradleSourceSets.class)
          .addArguments("--init-script", initScript.getAbsolutePath())
          .get();
      assertEquals(0, gradleSourceSets.getGradleSourceSets().size());
    }
  }

  @Test
  @EnabledOnJre({JRE.JAVA_8})
  void testGetOutputLocationFromOldGradle() throws IOException {
    File projectDir = projectPath.resolve("legacy-gradle").toFile();
    GradleConnector connector = GradleConnector.newConnector()
        .forProjectDirectory(projectDir)
        .useGradleVersion("5.6.4");
    try (ProjectConnection connect = connector.connect()) {
      File initScript = PluginHelper.getInitScript();
      GradleSourceSets gradleSourceSets = connect.model(GradleSourceSets.class)
          .addArguments("--init-script", initScript.getAbsolutePath())
          .get();
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
    }
  }

  @Test
  @EnabledOnJre({JRE.JAVA_8})
  void testGetAnnotationProcessorGeneratedLocation() throws IOException {
    // this test case is to ensure that the plugin won't throw no such method error
    // for JavaCompile.getAnnotationProcessorGeneratedSourcesDirectory()
    File projectDir = projectPath.resolve("legacy-gradle").toFile();
    GradleConnector connector = GradleConnector.newConnector()
        .forProjectDirectory(projectDir)
        .useGradleVersion("4.2.1");
    try (ProjectConnection connect = connector.connect()) {
      File initScript = PluginHelper.getInitScript();
      GradleSourceSets gradleSourceSets = connect.model(GradleSourceSets.class)
          .addArguments("--init-script", initScript.getAbsolutePath())
          .get();
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
    }
  }

  @Test
  void testSourceInference() throws IOException {
    File projectDir = projectPath.resolve("infer-source-roots").toFile();
    GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
    connector.useBuildDistribution();

    try (ProjectConnection connect = connector.connect()) {
      connect.newBuild().forTasks("clean", "compileJava").run();
      ModelBuilder<GradleSourceSets> modelBuilder = connect.model(GradleSourceSets.class);
      File initScript = PluginHelper.getInitScript();
      modelBuilder.addArguments("--init-script", initScript.getAbsolutePath());
      GradleSourceSets gradleSourceSets = modelBuilder.get();

      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      int generatedSourceDirCount = 0;
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        generatedSourceDirCount += gradleSourceSet.getGeneratedSourceDirs().size();
        assertTrue(gradleSourceSet.getGeneratedSourceDirs().stream().anyMatch(
            dir -> dir.getAbsolutePath().replaceAll("\\\\", "/").endsWith("build/generated/sources")
        ));
      }
      assertEquals(4, generatedSourceDirCount);
    }
  }
}
