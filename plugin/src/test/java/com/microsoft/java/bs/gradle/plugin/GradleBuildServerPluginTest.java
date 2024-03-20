// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.microsoft.java.bs.gradle.model.JavaExtension;
import com.microsoft.java.bs.gradle.model.ScalaExtension;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.SupportedLanguages;

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
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        assertNotNull(gradleSourceSet.getGradleVersion());
        assertEquals("junit5-jupiter-starter-gradle", gradleSourceSet.getProjectName());
        assertEquals(":", gradleSourceSet.getProjectPath());
        assertEquals(projectDir, gradleSourceSet.getProjectDir());
        assertEquals(projectDir, gradleSourceSet.getRootDir());
        assertTrue(gradleSourceSet.getSourceSetName().equals("main")
            || gradleSourceSet.getSourceSetName().equals("test"));
        assertTrue(gradleSourceSet.getClassesTaskName().equals("classes")
            || gradleSourceSet.getClassesTaskName().equals("testClasses"));
        assertFalse(gradleSourceSet.getCompileClasspath().isEmpty());
        assertTrue(gradleSourceSet.getSourceDirs().size() > 0);
        assertTrue(gradleSourceSet.getGeneratedSourceDirs().size() > 0);
        assertTrue(gradleSourceSet.getResourceDirs().size() > 0);
        assertNotNull(gradleSourceSet.getSourceOutputDir());
        assertNotNull(gradleSourceSet.getResourceOutputDir());

        assertNotNull(gradleSourceSet.getBuildTargetDependencies());
        assertNotNull(gradleSourceSet.getModuleDependencies());
        assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
            dependency -> dependency.getModule().equals("a.jar")
        ));
        assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
            dependency -> dependency.getModule().contains("gradle-api")
        ));

        JavaExtension javaExtension =
            SupportedLanguages.JAVA.convert(gradleSourceSet.getExtensions());
        assertNotNull(javaExtension);
        assertNotNull(javaExtension.getJavaHome());
        assertNotNull(javaExtension.getJavaVersion());
        assertNotNull(javaExtension.getSourceCompatibility());
        assertNotNull(javaExtension.getTargetCompatibility());
        assertNotNull(javaExtension.getCompilerArgs());
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
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
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
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
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
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
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
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);

      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      int generatedSourceDirCount = 0;
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        generatedSourceDirCount += gradleSourceSet.getGeneratedSourceDirs().size();
        assertTrue(gradleSourceSet.getGeneratedSourceDirs().stream().anyMatch(
            dir -> dir.getAbsolutePath().replaceAll("\\\\", "/")
                .endsWith("build/generated/sources")
        ));
      }
      assertEquals(4, generatedSourceDirCount);
    }
  }
  
  @Test
  void testJavaCompilerArgs1() throws IOException {
    File projectDir = projectPath.resolve("java-compilerargs-1").toFile();
    GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
    connector.useBuildDistribution();
    try (ProjectConnection connect = connector.connect()) {
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension =
            SupportedLanguages.JAVA.convert(gradleSourceSet.getExtensions());
        assertNotNull(javaExtension);
        String args = "|" + String.join("|", javaExtension.getCompilerArgs());
        assertFalse(args.contains("|--source|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--target|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--release|"), () -> "Available args: " + args);
        assertTrue(args.contains("|-source|1.8"), () -> "Available args: " + args);
        assertTrue(args.contains("|-target|9"), () -> "Available args: " + args);
        assertTrue(args.contains("|-Xlint:all"), () -> "Available args: " + args);
        assertEquals("1.8", javaExtension.getSourceCompatibility(),
            () -> "Available args: " + args);
        assertEquals("9", javaExtension.getTargetCompatibility(),
            () -> "Available args: " + args);
      }
    }
  }
  
  @Test
  void testJavaCompilerArgs2() throws IOException {
    File projectDir = projectPath.resolve("java-compilerargs-2").toFile();
    GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
    connector.useBuildDistribution();
    try (ProjectConnection connect = connector.connect()) {
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension =
            SupportedLanguages.JAVA.convert(gradleSourceSet.getExtensions());
        assertNotNull(javaExtension);
        String args = "|" + String.join("|", javaExtension.getCompilerArgs());
        assertFalse(args.contains("|--source|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--target|"), () -> "Available args: " + args);
        assertFalse(args.contains("|-source|"), () -> "Available args: " + args);
        assertFalse(args.contains("|-target|"), () -> "Available args: " + args);
        assertTrue(args.contains("|--release|9"), () -> "Available args: " + args);
        assertTrue(args.contains("|-Xlint:all"), () -> "Available args: " + args);
        assertEquals("9", javaExtension.getSourceCompatibility(),
            () -> "Available args: " + args);
        assertEquals("9", javaExtension.getTargetCompatibility(),
            () -> "Available args: " + args);
      }
    }
  }
  
  @Test
  void testJavaCompilerArgs3() throws IOException {
    File projectDir = projectPath.resolve("java-compilerargs-3").toFile();
    GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
    connector.useBuildDistribution();
    try (ProjectConnection connect = connector.connect()) {
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension =
            SupportedLanguages.JAVA.convert(gradleSourceSet.getExtensions());
        assertNotNull(javaExtension);
        String args = "|" + String.join("|", javaExtension.getCompilerArgs());
        assertFalse(args.contains("|--source|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--target|"), () -> "Available args: " + args);
        assertFalse(args.contains("|-source|"), () -> "Available args: " + args);
        assertFalse(args.contains("|-target|"), () -> "Available args: " + args);
        assertTrue(args.contains("|--release|9"), () -> "Available args: " + args);
        assertTrue(args.contains("|-Xlint:all"), () -> "Available args: " + args);
        assertEquals("9", javaExtension.getSourceCompatibility(),
            () -> "Available args: " + args);
        assertEquals("9", javaExtension.getTargetCompatibility(),
            () -> "Available args: " + args);
      }
    }
  }
  
  @Test
  void testJavaCompilerArgs4() throws IOException {
    File projectDir = projectPath.resolve("java-compilerargs-4").toFile();
    GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
    connector.useBuildDistribution();
    try (ProjectConnection connect = connector.connect()) {
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension =
            SupportedLanguages.JAVA.convert(gradleSourceSet.getExtensions());
        assertNotNull(javaExtension);
        String args = "|" + String.join("|", javaExtension.getCompilerArgs());
        assertFalse(args.contains("|--release|"), () -> "Available args: " + args);
        assertFalse(args.contains("|-source|"), () -> "Available args: " + args);
        assertFalse(args.contains("|-target|"), () -> "Available args: " + args);
        assertTrue(args.contains("|--source|1.8"), () -> "Available args: " + args);
        assertTrue(args.contains("|--target|9"), () -> "Available args: " + args);
        assertTrue(args.contains("|-Xlint:all"), () -> "Available args: " + args);
        assertEquals("1.8", javaExtension.getSourceCompatibility(),
            () -> "Available args: " + args);
        assertEquals("9", javaExtension.getTargetCompatibility(),
            () -> "Available args: " + args);
      }
    }
  }
  
  @Test
  void testJavaCompilerArgs5() throws IOException {
    File projectDir = projectPath.resolve("java-compilerargs-5").toFile();
    GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
    connector.useBuildDistribution();
    try (ProjectConnection connect = connector.connect()) {
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension =
            SupportedLanguages.JAVA.convert(gradleSourceSet.getExtensions());
        assertNotNull(javaExtension);
        String args = "|" + String.join("|", javaExtension.getCompilerArgs());
        assertFalse(args.contains("|--release|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--source|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--target|"), () -> "Available args: " + args);
        assertTrue(args.contains("|-source|1.8"), () -> "Available args: " + args);
        assertTrue(args.contains("|-target|9"), () -> "Available args: " + args);
        assertTrue(args.contains("|-Xlint:all"), () -> "Available args: " + args);
        assertEquals("1.8", javaExtension.getSourceCompatibility(),
            () -> "Available args: " + args);
        assertEquals("9", javaExtension.getTargetCompatibility(),
            () -> "Available args: " + args);
      }
    }
  }
  
  @Test
  void testJavaCompilerArgs6() throws IOException {
    File projectDir = projectPath.resolve("java-compilerargs-6").toFile();
    GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
    connector.useBuildDistribution();
    try (ProjectConnection connect = connector.connect()) {
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension =
            SupportedLanguages.JAVA.convert(gradleSourceSet.getExtensions());
        assertNotNull(javaExtension);
        String args = "|" + String.join("|", javaExtension.getCompilerArgs());
        assertFalse(args.contains("|--release|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--source|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--target|"), () -> "Available args: " + args);
        assertTrue(args.contains("|-source|"), () -> "Available args: " + args);
        assertTrue(args.contains("|-target|"), () -> "Available args: " + args);
        assertFalse(javaExtension.getSourceCompatibility().isEmpty(),
            () -> "Available args: " + args);
        assertFalse(javaExtension.getTargetCompatibility().isEmpty(),
            () -> "Available args: " + args);
      }
    }
  }

  @Test
  void testJavaCompilerArgsToolchain() throws IOException {
    File projectDir = projectPath.resolve("java-compilerargs-toolchain").toFile();
    GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
    connector.useBuildDistribution();
    try (ProjectConnection connect = connector.connect()) {
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension =
            SupportedLanguages.JAVA.convert(gradleSourceSet.getExtensions());
        assertNotNull(javaExtension);
        String args = "|" + String.join("|", javaExtension.getCompilerArgs());
        assertFalse(args.contains("|--release|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--source|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--target|"), () -> "Available args: " + args);
        assertTrue(args.contains("|-source|17|"), () -> "Available args: " + args);
        assertTrue(args.contains("|-target|17|"), () -> "Available args: " + args);
        assertFalse(javaExtension.getSourceCompatibility().isEmpty(),
            () -> "Available args: " + args);
        assertFalse(javaExtension.getTargetCompatibility().isEmpty(),
            () -> "Available args: " + args);
      }
    }
  }

  private GradleSourceSets getGradleSourceSets(ProjectConnection connect) throws IOException {
    ModelBuilder<GradleSourceSets> modelBuilder = connect.model(GradleSourceSets.class);
    File initScript = PluginHelper.getInitScript();
    modelBuilder.addArguments("--init-script", initScript.getAbsolutePath());
    modelBuilder.addJvmArguments("-Dbsp.gradle.supportedLanguages=java,scala");
    return new DefaultGradleSourceSets(modelBuilder.get());
  }

  @Test
  void testScala2ModelBuilder() throws IOException {
    File projectDir = projectPath.resolve("scala-2").toFile();
    GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
    connector.useBuildDistribution();
    try (ProjectConnection connect = connector.connect()) {
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        assertEquals("scala-2", gradleSourceSet.getProjectName());
        assertEquals(":", gradleSourceSet.getProjectPath());
        assertEquals(projectDir, gradleSourceSet.getProjectDir());
        assertEquals(projectDir, gradleSourceSet.getRootDir());
        assertTrue(gradleSourceSet.getSourceSetName().equals("main")
                || gradleSourceSet.getSourceSetName().equals("test"));
        assertTrue(gradleSourceSet.getClassesTaskName().equals("classes")
                || gradleSourceSet.getClassesTaskName().equals("testClasses"));
        assertFalse(gradleSourceSet.getCompileClasspath().isEmpty());
        assertTrue(gradleSourceSet.getSourceDirs().size() > 0);
        assertTrue(gradleSourceSet.getGeneratedSourceDirs().size() > 0);
        assertTrue(gradleSourceSet.getResourceDirs().size() > 0);
        assertNotNull(gradleSourceSet.getBuildTargetDependencies());
        assertNotNull(gradleSourceSet.getModuleDependencies());
        assertNotNull(gradleSourceSet.getSourceOutputDir());
        assertNotNull(gradleSourceSet.getResourceOutputDir());

        JavaExtension javaExtension =
            SupportedLanguages.JAVA.convert(gradleSourceSet.getExtensions());
        assertNotNull(javaExtension);
        assertNotNull(javaExtension.getJavaHome());
        assertNotNull(javaExtension.getJavaVersion());

        assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
                dependency -> dependency.getModule().equals("scala-library")
        ));
        ScalaExtension scalaExtension =
             SupportedLanguages.SCALA.convert(gradleSourceSet.getExtensions());
        assertNotNull(scalaExtension);
        assertEquals("org.scala-lang", scalaExtension.getScalaOrganization());
        assertEquals("2.13.12", scalaExtension.getScalaVersion());
        assertEquals("2.13", scalaExtension.getScalaBinaryVersion());

        assertTrue(gradleSourceSet.getCompileClasspath().stream().anyMatch(
                file -> file.getName().equals("scala-library-2.13.12.jar")));
        assertFalse(scalaExtension.getScalaJars().isEmpty());
        assertTrue(scalaExtension.getScalaJars().stream().anyMatch(
                file -> file.getName().equals("scala-compiler-2.13.12.jar")));
        assertFalse(scalaExtension.getScalaCompilerArgs().isEmpty());
        assertTrue(scalaExtension.getScalaCompilerArgs().stream()
                .anyMatch(arg -> arg.equals("-deprecation")));
      }
    }
  }

  @Test
  void testScala3ModelBuilder() throws IOException {
    File projectDir = projectPath.resolve("scala-3").toFile();
    GradleConnector connector = GradleConnector.newConnector().forProjectDirectory(projectDir);
    connector.useBuildDistribution();
    try (ProjectConnection connect = connector.connect()) {
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        assertEquals("scala-3", gradleSourceSet.getProjectName());
        assertEquals(":", gradleSourceSet.getProjectPath());
        assertEquals(projectDir, gradleSourceSet.getProjectDir());
        assertEquals(projectDir, gradleSourceSet.getRootDir());
        assertTrue(gradleSourceSet.getSourceSetName().equals("main")
                || gradleSourceSet.getSourceSetName().equals("test"));
        assertTrue(gradleSourceSet.getClassesTaskName().equals("classes")
                || gradleSourceSet.getClassesTaskName().equals("testClasses"));
        assertFalse(gradleSourceSet.getCompileClasspath().isEmpty());
        assertTrue(gradleSourceSet.getSourceDirs().size() > 0);
        assertTrue(gradleSourceSet.getGeneratedSourceDirs().size() > 0);
        assertTrue(gradleSourceSet.getResourceDirs().size() > 0);
        assertNotNull(gradleSourceSet.getSourceOutputDir());
        assertNotNull(gradleSourceSet.getResourceOutputDir());
        assertNotNull(gradleSourceSet.getBuildTargetDependencies());
        assertNotNull(gradleSourceSet.getModuleDependencies());
        JavaExtension javaExtension =
            SupportedLanguages.JAVA.convert(gradleSourceSet.getExtensions());
        assertNotNull(javaExtension);
        assertNotNull(javaExtension.getJavaHome());
        assertNotNull(javaExtension.getJavaVersion());

        assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
                dependency -> dependency.getModule().contains("scala3-library_3")
        ));

        ScalaExtension scalaExtension =
             SupportedLanguages.SCALA.convert(gradleSourceSet.getExtensions());
        assertNotNull(scalaExtension);
        assertEquals("org.scala-lang", scalaExtension.getScalaOrganization());
        assertEquals("3.3.1", scalaExtension.getScalaVersion());
        assertEquals("3.3", scalaExtension.getScalaBinaryVersion());

        assertTrue(gradleSourceSet.getCompileClasspath().stream().anyMatch(
                file -> file.getName().equals("scala3-library_3-3.3.1.jar")));
        assertFalse(scalaExtension.getScalaJars().isEmpty());
        assertTrue(scalaExtension.getScalaJars().stream().anyMatch(
                file -> file.getName().equals("scala3-compiler_3-3.3.1.jar")));
        assertFalse(scalaExtension.getScalaCompilerArgs().isEmpty());
        assertTrue(scalaExtension.getScalaCompilerArgs().stream()
                .anyMatch(arg -> arg.equals("-deprecation")));
      }
    }
  }
}
