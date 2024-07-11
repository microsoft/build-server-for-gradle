// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.gradle.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.ArrayList;

import com.microsoft.java.bs.gradle.model.GradleSourceSet;
import com.microsoft.java.bs.gradle.model.GradleSourceSets;
import com.microsoft.java.bs.gradle.model.JavaExtension;
import com.microsoft.java.bs.gradle.model.ScalaExtension;
import com.microsoft.java.bs.gradle.model.SupportedLanguages;
import com.microsoft.java.bs.gradle.model.actions.GetSourceSetsAction;
import com.microsoft.java.bs.gradle.model.impl.DefaultGradleSourceSets;

import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

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

  private GradleSourceSets getGradleSourceSets(ProjectConnection connect) throws IOException {
    BuildActionExecuter<GradleSourceSets> action = connect.action(new GetSourceSetsAction());
    File initScript = PluginHelper.getInitScript();
    action
        .addArguments("--init-script", initScript.getAbsolutePath())
        .addArguments("-Dorg.gradle.daemon.idletimeout=10")
        .addArguments("-Dorg.gradle.vfs.watch=false")
        .addArguments("-Dorg.gradle.logging.level=quiet")
        .addJvmArguments("-Dbsp.gradle.supportedLanguages="
            + String.join(",", SupportedLanguages.allBspNames));
    GradleSourceSets sourceSets = action.run();
    return new DefaultGradleSourceSets(new ArrayList<>(sourceSets.getGradleSourceSets()));
  }

  private interface ConnectionConsumer {
    void accept(ProjectConnection connection) throws IOException;
  }

  private void withConnection(File projectDir, GradleVersion gradleVersion,
      ConnectionConsumer consumer) throws IOException {
    GradleConnector connector = GradleConnector.newConnector()
        .forProjectDirectory(projectDir)
        .useGradleVersion(gradleVersion.getVersion());
    try (ProjectConnection connect = connector.connect()) {
      consumer.accept(connect);
    } finally {
      connector.disconnect();
    }
  }

  private void withSourceSets(String projectName, GradleVersion gradleVersion,
      Consumer<GradleSourceSets> consumer) throws IOException {
    File projectDir = projectPath.resolve(projectName).toFile();
    withConnection(projectDir, gradleVersion, connect -> {
      GradleSourceSets gradleSourceSets = getGradleSourceSets(connect);
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        assertEquals(gradleVersion.getVersion(), gradleSourceSet.getGradleVersion());
        assertEquals(projectName, gradleSourceSet.getProjectName());
        assertEquals(projectDir, gradleSourceSet.getProjectDir());
        assertEquals(projectDir, gradleSourceSet.getRootDir());
      }
      consumer.accept(gradleSourceSets);
    });
  }

  private static int getJavaVersion() {
    String version = System.getProperty("java.version");
    if (version.startsWith("1.")) {
      version = version.substring(2, 3);
    } else {
      int dot = version.indexOf(".");
      if (dot != -1) {
        version = version.substring(0, dot);
      }
    }
    return Integer.parseInt(version);
  }

  private static class GradleJreVersion {
    final String gradleVersion;
    final int jreVersion;

    GradleJreVersion(String gradleVersion, int jreVersion) {
      this.gradleVersion = gradleVersion;
      this.jreVersion = jreVersion;
    }

    GradleVersion getGradleVersion() {
      return GradleVersion.version(gradleVersion);
    }
  }
  
  /**
   * create a list of gradle versions that work with the runtime JRE.
   */
  static Stream<GradleVersion> versionProvider() {
    int javaVersion = getJavaVersion();
    // change the last version in the below list to point to the highest Gradle version supported
    // if the Gradle API changes then keep that version forever and add a comment as to why
    return Stream.of(
      // earliest supported version
      new GradleJreVersion("2.12", 8),
      // java source/target options specified in 2.14
      // tooling api jar name changed from gradle-tooling-api to gradle-api in 3.0
      new GradleJreVersion("3.0", 8),
      // artifacts view added in 4.0
      // RuntimeClasspathConfigurationName added to sourceset in 3.4
      // Test#getTestClassesDir -> Test#getTestClassesDirs in 4.0
      // sourceSet#getJava#getOutputDir added in 4.0
      new GradleJreVersion("4.2.1", 8),
      // CompileOptions#getAnnotationProcessorGeneratedSourcesDirectory added in 4.3
      new GradleJreVersion("4.3.1", 9),
      // SourceSetContainer added to project#getExtensions in 5.0
      new GradleJreVersion("5.0", 11),
      // AbstractArchiveTask#getArchiveFile -> AbstractArchiveTask#getArchiveFile in 5.1
      // annotation processor dirs auto created in 5.2
      new GradleJreVersion("5.2", 11),
      // sourceSet#getJava#getOutputDir -> sourceSet#getJava#getClassesDirectory in 6.1
      new GradleJreVersion("6.1", 13),
      // DefaultCopySpec#getChildren changed from Iterable to Collection in 6.2
      new GradleJreVersion("6.2", 13),
      // CompileOptions#getGeneratedSourceOutputDirectory added in 6.3
      new GradleJreVersion("6.3", 14),
      // CompileOptions#getRelease added in 6.6
      new GradleJreVersion("6.6", 13),
      // ScalaSourceDirectorySet added to project#getExtensions in 7.1
      new GradleJreVersion("7.1", 16),
      // Scala 3 support added in 7.3
      new GradleJreVersion("7.3", 17),
      // FoojayToolchainsPlugin requires >= 7.6
      new GradleJreVersion("7.6.1", 19),
      // JDK source/target options changed from 1.9 -> 9 in 8.0
      new GradleJreVersion("8.0", 19),
      // highest supported version
      new GradleJreVersion("8.8", 22)
    ).filter(version -> version.jreVersion >= javaVersion)
     .map(GradleJreVersion::getGradleVersion);
  }

  @ParameterizedTest(name = "testModelBuilder {0}")
  @MethodSource("versionProvider")
  void testModelBuilder(GradleVersion gradleVersion) throws IOException {
    withSourceSets("junit5-jupiter-starter-gradle", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        assertEquals(":", gradleSourceSet.getProjectPath());
        assertTrue(gradleSourceSet.getSourceSetName().equals("main")
            || gradleSourceSet.getSourceSetName().equals("test"));
        assertTrue(gradleSourceSet.getClassesTaskName().equals(":classes")
            || gradleSourceSet.getClassesTaskName().equals(":testClasses"));
        assertFalse(gradleSourceSet.getCompileClasspath().isEmpty());
        assertEquals(1, gradleSourceSet.getSourceDirs().size());
        // annotation processor dirs weren't auto created before 5.2
        if (gradleVersion.compareTo(GradleVersion.version("5.2")) >= 0) {
          assertEquals(1, gradleSourceSet.getGeneratedSourceDirs().size());
        }
        assertEquals(1, gradleSourceSet.getResourceDirs().size());
        assertNotNull(gradleSourceSet.getSourceOutputDirs());
        assertNotNull(gradleSourceSet.getResourceOutputDir());

        assertNotNull(gradleSourceSet.getBuildTargetDependencies());
        assertNotNull(gradleSourceSet.getModuleDependencies());
        assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
            dependency -> dependency.getModule().equals("a.jar")
        ));

        if (gradleVersion.compareTo(GradleVersion.version("3.0")) >= 0) {
          assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
              dependency -> dependency.getModule().contains("gradle-api")
          ));
        } else {
          assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
              dependency -> dependency.getModule().contains("gradle-tooling-api")
          ));
        }

        JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(gradleSourceSet);
        assertNotNull(javaExtension);
        assertNotNull(javaExtension.getJavaHome());
        assertNotNull(javaExtension.getJavaVersion());
        assertNotNull(javaExtension.getSourceCompatibility());
        assertNotNull(javaExtension.getTargetCompatibility());
        assertNotNull(javaExtension.getCompilerArgs());
        
        // dirs not split by language before 4.0
        if (gradleVersion.compareTo(GradleVersion.version("4.0")) >= 0) {
          assertTrue(gradleSourceSet.getSourceOutputDirs().stream()
              .anyMatch(file -> file.toPath().endsWith(Paths.get("classes", "java",
              gradleSourceSet.getSourceSetName()))));
          assertTrue(javaExtension.getClassesDir().toPath().endsWith(Paths.get("classes", "java",
              gradleSourceSet.getSourceSetName())));
        } else {
          assertTrue(gradleSourceSet.getSourceOutputDirs().stream()
              .anyMatch(file -> file.toPath().endsWith(Paths.get("classes",
              gradleSourceSet.getSourceSetName()))));
          assertTrue(javaExtension.getClassesDir().toPath().endsWith(Paths.get("classes",
              gradleSourceSet.getSourceSetName())));
        }
      }
    });
  }

  @ParameterizedTest(name = "testGetSourceContainerFromOldGradle {0}")
  @MethodSource("versionProvider")
  void testMissingRepository(GradleVersion gradleVersion) throws IOException {
    withSourceSets("missing-repository", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
    });
  }

  @ParameterizedTest(name = "testGetSourceContainerFromOldGradle {0}")
  @MethodSource("versionProvider")
  void testGetSourceContainerFromOldGradle(GradleVersion gradleVersion) throws IOException {
    withSourceSets("non-java", gradleVersion, gradleSourceSets -> {
      assertEquals(0, gradleSourceSets.getGradleSourceSets().size());
    });
  }

  @ParameterizedTest(name = "testGetOutputLocationFromOldGradle {0}")
  @MethodSource("versionProvider")
  void testGetOutputLocationFromOldGradle(GradleVersion gradleVersion) throws IOException {
    withSourceSets("legacy-gradle", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
    });
  }

  @ParameterizedTest(name = "testGetAnnotationProcessorGeneratedLocation {0}")
  @MethodSource("versionProvider")
  void testGetAnnotationProcessorGeneratedLocation(GradleVersion gradleVersion) throws IOException {
    // this test case is to ensure that the plugin won't throw no such method error
    // for JavaCompile.getAnnotationProcessorGeneratedSourcesDirectory()
    withSourceSets("legacy-gradle", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
    });
  }

  @ParameterizedTest(name = "testSourceInference {0}")
  @MethodSource("versionProvider")
  void testSourceInference(GradleVersion gradleVersion) throws IOException {
    File projectDir = projectPath.resolve("infer-source-roots").toFile();
    withConnection(projectDir, gradleVersion, connect -> {
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
      
      // annotation processor dirs weren't auto created before 5.2
      if (gradleVersion.compareTo(GradleVersion.version("5.2")) >= 0) {
        assertEquals(4, generatedSourceDirCount);
      } else {
        assertEquals(2, generatedSourceDirCount);
      }
    });
  }

  @ParameterizedTest(name = "testJavaCompilerArgs1 {0}")
  @MethodSource("versionProvider")
  void testJavaCompilerArgs1(GradleVersion gradleVersion) throws IOException {
    // Gradle uses 1.9 in earlier versions to indicate JDK 9
    final String targetVersion;
    if (gradleVersion.compareTo(GradleVersion.version("8.0")) >= 0) {
      targetVersion = "9";
    } else {
      targetVersion = "1.9";
    }
    withSourceSets("java-compilerargs-1", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(gradleSourceSet);
        assertNotNull(javaExtension);
        String args = "|" + String.join("|", javaExtension.getCompilerArgs());
        assertFalse(args.contains("|--source|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--target|"), () -> "Available args: " + args);
        assertFalse(args.contains("|--release|"), () -> "Available args: " + args);
        if (gradleVersion.compareTo(GradleVersion.version("3.0")) >= 0) {
          assertTrue(args.contains("|-source|1.8"), () -> "Available args: " + args);
        }
        assertTrue(args.contains("|-target|" + targetVersion), () -> "Available args: " + args);
        assertTrue(args.contains("|-Xlint:all"), () -> "Available args: " + args);
        if (gradleVersion.compareTo(GradleVersion.version("3.0")) >= 0) {
          assertEquals("1.8", javaExtension.getSourceCompatibility(),
              () -> "Available args: " + args);
        }
        assertEquals(targetVersion, javaExtension.getTargetCompatibility(),
            () -> "Available args: " + args);
      }
    });
  }

  @ParameterizedTest(name = "testJavaCompilerArgs2 {0}")
  @MethodSource("versionProvider")
  void testJavaCompilerArgs2(GradleVersion gradleVersion) throws IOException {
    // JavaCompile#options#release was added in Gradle 6.6
    assumeTrue(gradleVersion.compareTo(GradleVersion.version("6.6")) >= 0);
    withSourceSets("java-compilerargs-2", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(gradleSourceSet);
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
    });
  }

  @ParameterizedTest(name = "testJavaCompilerArgs3 {0}")
  @MethodSource("versionProvider")
  void testJavaCompilerArgs3(GradleVersion gradleVersion) throws IOException {
    withSourceSets("java-compilerargs-3", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(gradleSourceSet);
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
    });
  }

  @ParameterizedTest(name = "testJavaCompilerArgs4 {0}")
  @MethodSource("versionProvider")
  void testJavaCompilerArgs4(GradleVersion gradleVersion) throws IOException {
    withSourceSets("java-compilerargs-4", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(gradleSourceSet);
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
    });
  }

  @ParameterizedTest(name = "testJavaCompilerArgs5 {0}")
  @MethodSource("versionProvider")
  void testJavaCompilerArgs5(GradleVersion gradleVersion) throws IOException {
    withSourceSets("java-compilerargs-5", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(gradleSourceSet);
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
    });
  }

  @ParameterizedTest(name = "testJavaCompilerArgs6 {0}")
  @MethodSource("versionProvider")
  void testJavaCompilerArgs6(GradleVersion gradleVersion) throws IOException {
    // Gradle doesn't set source/target unless specified until version 2.14
    assumeTrue(gradleVersion.compareTo(GradleVersion.version("2.14")) >= 0);
    withSourceSets("java-compilerargs-6", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(gradleSourceSet);
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
    });
  }

  @ParameterizedTest(name = "testJavaCompilerArgsToolchain {0}")
  @MethodSource("versionProvider")
  void testJavaCompilerArgsToolchain(GradleVersion gradleVersion) throws IOException {
    // FoojayToolchainsPlugin needs Gradle version 7.6 or higher
    assumeTrue(gradleVersion.compareTo(GradleVersion.version("7.6")) >= 0);
    withSourceSets("java-compilerargs-toolchain", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(gradleSourceSet);
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
    });
  }

  @ParameterizedTest(name = "testScala2ModelBuilder {0}")
  @MethodSource("versionProvider")
  void testScala2ModelBuilder(GradleVersion gradleVersion) throws IOException {
    withSourceSets("scala-2", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        assertEquals(":", gradleSourceSet.getProjectPath());
        assertTrue(gradleSourceSet.getSourceSetName().equals("main")
                || gradleSourceSet.getSourceSetName().equals("test"));
        assertTrue(gradleSourceSet.getClassesTaskName().equals(":classes")
                || gradleSourceSet.getClassesTaskName().equals(":testClasses"));
        assertFalse(gradleSourceSet.getCompileClasspath().isEmpty());
        assertEquals(2, gradleSourceSet.getSourceDirs().size());
        // annotation processor dirs weren't auto created before 5.2
        if (gradleVersion.compareTo(GradleVersion.version("5.2")) >= 0) {
          assertEquals(1, gradleSourceSet.getGeneratedSourceDirs().size());
        }
        assertEquals(1, gradleSourceSet.getResourceDirs().size());
        assertNotNull(gradleSourceSet.getBuildTargetDependencies());
        assertNotNull(gradleSourceSet.getModuleDependencies());
        assertNotNull(gradleSourceSet.getSourceOutputDirs());
        assertNotNull(gradleSourceSet.getResourceOutputDir());

        JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(gradleSourceSet);
        assertNotNull(javaExtension);
        assertNotNull(javaExtension.getJavaHome());
        assertNotNull(javaExtension.getJavaVersion());

        assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
                dependency -> dependency.getModule().equals("scala-library")
        ));
        assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
            dependency -> dependency.getArtifacts().stream()
              .anyMatch(artifact -> artifact.getUri().toString()
                .contains("scala-library-2.13.12.jar"))
        ));
        ScalaExtension scalaExtension = SupportedLanguages.SCALA.getExtension(gradleSourceSet);
        assertNotNull(scalaExtension);
        assertEquals("org.scala-lang", scalaExtension.getScalaOrganization());
        assertEquals("2.13.12", scalaExtension.getScalaVersion());
        assertEquals("2.13", scalaExtension.getScalaBinaryVersion());
        List<String> args = scalaExtension.getScalaCompilerArgs();
        assertTrue(args.contains("-deprecation"), () -> "Available args: " + args);
        assertTrue(args.contains("-unchecked"), () -> "Available args: " + args);
        assertTrue(args.contains("-g:notailcalls"), () -> "Available args: " + args);
        assertTrue(args.contains("-optimise"), () -> "Available args: " + args);
        assertTrue(args.contains("-encoding"), () -> "Available args: " + args);
        assertTrue(args.contains("utf8"), () -> "Available args: " + args);
        assertTrue(args.contains("-verbose"), () -> "Available args: " + args);
        assertTrue(args.contains("-Ylog:erasure"), () -> "Available args: " + args);
        assertTrue(args.contains("-Ylog:lambdalift"), () -> "Available args: " + args);
        assertTrue(args.contains("-foo"), () -> "Available args: " + args);

        assertTrue(gradleSourceSet.getCompileClasspath().stream().anyMatch(
                file -> file.getName().equals("scala-library-2.13.12.jar")));
        assertFalse(scalaExtension.getScalaJars().isEmpty());
        assertTrue(scalaExtension.getScalaJars().stream().anyMatch(
                file -> file.getName().equals("scala-compiler-2.13.12.jar")));
        assertFalse(scalaExtension.getScalaCompilerArgs().isEmpty());
        assertTrue(scalaExtension.getScalaCompilerArgs().stream()
                .anyMatch(arg -> arg.equals("-deprecation")));

        // dirs not split by language before 4.0
        if (gradleVersion.compareTo(GradleVersion.version("4.0")) >= 0) {
          assertTrue(gradleSourceSet.getSourceOutputDirs().stream()
              .anyMatch(file -> file.toPath().endsWith(Paths.get("classes", "java",
              gradleSourceSet.getSourceSetName()))));
          assertTrue(gradleSourceSet.getSourceOutputDirs().stream()
              .anyMatch(file -> file.toPath().endsWith(Paths.get("classes", "scala",
              gradleSourceSet.getSourceSetName()))));
          assertTrue(javaExtension.getClassesDir().toPath().endsWith(Paths.get("classes", "java",
              gradleSourceSet.getSourceSetName())));
          assertTrue(scalaExtension.getClassesDir().toPath().endsWith(Paths.get("classes", "scala",
              gradleSourceSet.getSourceSetName())));
        } else {
          assertTrue(gradleSourceSet.getSourceOutputDirs().stream()
              .anyMatch(file -> file.toPath().endsWith(Paths.get("classes",
              gradleSourceSet.getSourceSetName()))));
          assertTrue(scalaExtension.getClassesDir().toPath().endsWith(Paths.get("classes",
              gradleSourceSet.getSourceSetName())));
          assertTrue(javaExtension.getClassesDir().toPath().endsWith(Paths.get("classes",
              gradleSourceSet.getSourceSetName())));
        }
      }
    });
  }

  @ParameterizedTest(name = "testScala3ModelBuilder {0}")
  @MethodSource("versionProvider")
  void testScala3ModelBuilder(GradleVersion gradleVersion) throws IOException {
    // Scala 3 was added in Gradle 7.3
    assumeTrue(gradleVersion.compareTo(GradleVersion.version("7.3")) >= 0);
    withSourceSets("scala-3", gradleVersion, gradleSourceSets -> {
      assertEquals(2, gradleSourceSets.getGradleSourceSets().size());
      for (GradleSourceSet gradleSourceSet : gradleSourceSets.getGradleSourceSets()) {
        assertEquals(":", gradleSourceSet.getProjectPath());
        assertTrue(gradleSourceSet.getSourceSetName().equals("main")
                || gradleSourceSet.getSourceSetName().equals("test"));
        assertTrue(gradleSourceSet.getClassesTaskName().equals(":classes")
                || gradleSourceSet.getClassesTaskName().equals(":testClasses"));
        assertFalse(gradleSourceSet.getCompileClasspath().isEmpty());
        assertEquals(2, gradleSourceSet.getSourceDirs().size());
        // annotation processor dirs weren't auto created before 5.2
        if (gradleVersion.compareTo(GradleVersion.version("5.2")) >= 0) {
          assertEquals(1, gradleSourceSet.getGeneratedSourceDirs().size());
        }
        assertEquals(1, gradleSourceSet.getResourceDirs().size());
        assertNotNull(gradleSourceSet.getSourceOutputDirs());
        assertNotNull(gradleSourceSet.getResourceOutputDir());
        assertNotNull(gradleSourceSet.getBuildTargetDependencies());
        assertNotNull(gradleSourceSet.getModuleDependencies());
        JavaExtension javaExtension = SupportedLanguages.JAVA.getExtension(gradleSourceSet);
        assertNotNull(javaExtension);
        assertNotNull(javaExtension.getJavaHome());
        assertNotNull(javaExtension.getJavaVersion());

        assertTrue(gradleSourceSet.getModuleDependencies().stream().anyMatch(
                dependency -> dependency.getModule().contains("scala3-library_3")
        ));

        ScalaExtension scalaExtension = SupportedLanguages.SCALA.getExtension(gradleSourceSet);
        assertNotNull(scalaExtension);
        assertEquals("org.scala-lang", scalaExtension.getScalaOrganization());
        assertEquals("3.3.1", scalaExtension.getScalaVersion());
        assertEquals("3.3", scalaExtension.getScalaBinaryVersion());
        List<String> args = scalaExtension.getScalaCompilerArgs();
        assertTrue(args.contains("-deprecation"), () -> "Available args: " + args);
        assertTrue(args.contains("-unchecked"), () -> "Available args: " + args);
        assertTrue(args.contains("-g:notailcalls"), () -> "Available args: " + args);
        assertTrue(args.contains("-optimise"), () -> "Available args: " + args);
        assertTrue(args.contains("-encoding"), () -> "Available args: " + args);
        assertTrue(args.contains("utf8"), () -> "Available args: " + args);
        assertTrue(args.contains("-verbose"), () -> "Available args: " + args);
        assertTrue(args.contains("-Ylog:erasure"), () -> "Available args: " + args);
        assertTrue(args.contains("-Ylog:lambdalift"), () -> "Available args: " + args);
        assertTrue(args.contains("-foo"), () -> "Available args: " + args);

        assertTrue(gradleSourceSet.getCompileClasspath().stream().anyMatch(
                file -> file.getName().equals("scala3-library_3-3.3.1.jar")));
        assertFalse(scalaExtension.getScalaJars().isEmpty());
        assertTrue(scalaExtension.getScalaJars().stream().anyMatch(
                file -> file.getName().equals("scala3-compiler_3-3.3.1.jar")));
        assertFalse(scalaExtension.getScalaCompilerArgs().isEmpty());
        assertTrue(scalaExtension.getScalaCompilerArgs().stream()
                .anyMatch(arg -> arg.equals("-deprecation")));

        assertTrue(gradleSourceSet.getSourceOutputDirs().stream()
            .anyMatch(file -> file.toPath().endsWith(Paths.get("classes", "java",
            gradleSourceSet.getSourceSetName()))));
        assertTrue(gradleSourceSet.getSourceOutputDirs().stream()
            .anyMatch(file -> file.toPath().endsWith(Paths.get("classes", "scala",
            gradleSourceSet.getSourceSetName()))));
        assertTrue(javaExtension.getClassesDir().toPath().endsWith(Paths.get("classes", "java",
            gradleSourceSet.getSourceSetName())));
        assertTrue(scalaExtension.getClassesDir().toPath().endsWith(Paths.get("classes", "scala",
            gradleSourceSet.getSourceSetName())));
      }
    });
  }
}
