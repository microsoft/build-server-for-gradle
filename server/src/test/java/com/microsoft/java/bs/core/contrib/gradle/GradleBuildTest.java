package com.microsoft.java.bs.core.contrib.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTarget;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTargets;
import com.microsoft.java.bs.core.BspModule;
import com.microsoft.java.bs.core.bsp.BuildServerStatus;
import com.microsoft.java.bs.core.managers.BuildTargetsManager;
import com.microsoft.java.bs.core.managers.PreferencesManager;
import com.microsoft.java.bs.core.model.Preferences;

import ch.epfl.scala.bsp4j.BuildTarget;
import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Tests for {@link GradleBuild}.
 */
class GradleBuildTest {

  private static Path projectPath;

  private Injector injector;

  @BeforeEach
  void setUp() {
    injector = Guice.createInjector(new BspModule());
    PreferencesManager manager = injector.getInstance(PreferencesManager.class);
    manager.setPreferences(new Preferences());
  }

  @BeforeAll
  static void beforeClass() {
    projectPath = Paths.get(
        System.getProperty("user.dir"),
        "..",
        "testProjects"
    );
  }

  @Test
  void testGetSourceSetEntries() throws Exception {
    File projectDir = projectPath.resolve("junit5-jupiter-starter-gradle").toFile();
    GradleBuild gradleBuild = injector.getInstance(GradleBuild.class);
    JavaBuildTargets sourceSetEntries = gradleBuild.getSourceSetEntries(projectDir.toURI());
    List<JavaBuildTarget> javaBuildTargets = sourceSetEntries.getJavaBuildTargets();

    assertEquals(2, javaBuildTargets.size());
    for (JavaBuildTarget javaBuildTarget : javaBuildTargets) {
      assertEquals(1, javaBuildTarget.getSourceDirs().size());
      if (Objects.equals(javaBuildTarget.getSourceSetName(), "test")) {
        assertEquals(8, javaBuildTarget.getModuleDependencies().size());
        assertEquals(0, javaBuildTarget.getProjectDependencies().size());
      }
    }
  }

  @Test
  void testGetSourceSetEntriesAfterFileGenerated() throws Exception {
    File projectDir = projectPath.resolve("infer-source-roots").toFile();
    ProjectConnection connect = GradleConnector.newConnector().forProjectDirectory(projectDir)
        .connect();
    connect.newBuild().forTasks("clean").run();

    GradleBuild gradleBuild = injector.getInstance(GradleBuild.class);
    JavaBuildTargets sourceSetEntries = gradleBuild.getSourceSetEntries(projectDir.toURI());
    List<JavaBuildTarget> javaBuildTargets = sourceSetEntries.getJavaBuildTargets();
    int sourceDirCount = 0;
    for (JavaBuildTarget javaBuildTarget : javaBuildTargets) {
      sourceDirCount += javaBuildTarget.getSourceDirs().size();
    }
    assertEquals(2, sourceDirCount);

    // Generate a new file
    connect = GradleConnector.newConnector().forProjectDirectory(projectDir)
        .connect();
    connect.newBuild().forTasks("compileJava").run();

    sourceSetEntries = gradleBuild.getSourceSetEntries(projectDir.toURI());
    javaBuildTargets = sourceSetEntries.getJavaBuildTargets();
    sourceDirCount = 0;
    for (JavaBuildTarget javaBuildTarget : javaBuildTargets) {
      sourceDirCount += javaBuildTarget.getSourceDirs().size();
    }
    assertEquals(4, sourceDirCount);
  }

  @Test
  void testResolveToolingApiDependency() {
    File projectDir = projectPath.resolve("gradle-test-plugin").toFile();
    GradleBuild gradleBuild = injector.getInstance(GradleBuild.class);
    JavaBuildTargets sourceSetEntries = gradleBuild.getSourceSetEntries(projectDir.toURI());
    List<JavaBuildTarget> javaBuildTargets = sourceSetEntries.getJavaBuildTargets();

    for (JavaBuildTarget javaBuildTarget : javaBuildTargets) {
      assertEquals(1, javaBuildTarget.getSourceDirs().size());
      if (Objects.equals(javaBuildTarget.getSourceSetName(), "main")) {
        assertEquals(1, javaBuildTarget.getSourceDirs().size());
        assertTrue(javaBuildTarget.getModuleDependencies().size() > 0);
      }
    }
  }

  @Test
  void testCompilationFailure() throws IOException {
    File projectDir = projectPath.resolve("junit5-jupiter-starter-gradle").toFile();
    Path filePath = projectDir.toPath()
        .resolve("src/main/java/com/example/project/Calculator.java");
    try {
      replaceContent(filePath, "return a + b;", "return a + b.");

      BuildTargetsManager manager = injector.getInstance(BuildTargetsManager.class);
      BuildServerStatus buildServerStatus = injector.getInstance(BuildServerStatus.class);
      buildServerStatus.setRootUri(projectDir.toURI());
      manager.initialize();

      BuildTargetIdentifier btId = manager.getBuildTargets().iterator().next().getId();
      GradleBuild gradleBuild = injector.getInstance(GradleBuild.class);
      assertEquals(StatusCode.ERROR, gradleBuild.build(Arrays.asList(btId)));
    } finally {
      replaceContent(filePath, "return a + b.", "return a + b;");
    }
  }

  @Test
  void testClean() {
    File projectDir = projectPath.resolve("junit5-jupiter-starter-gradle").toFile();
    GradleBuild gradleBuild = injector.getInstance(GradleBuild.class);
    BuildTargetIdentifier btId = new BuildTargetIdentifier(projectDir.toURI().toString());
    File outputDir = projectDir.toPath().resolve("build").toFile();
    outputDir.mkdirs();

    boolean cleanCache = gradleBuild.cleanCache(Arrays.asList(btId));;

    assertTrue(cleanCache);
    assertFalse(outputDir.exists());
  }

  @Test
  void testMultiSettingsGradleProject() {
    File projectDir = projectPath.resolve("multiple-settings-gradle").toFile();
    BuildTargetsManager manager = injector.getInstance(BuildTargetsManager.class);
    BuildServerStatus buildServerStatus = injector.getInstance(BuildServerStatus.class);
    buildServerStatus.setRootUri(projectDir.toURI());
    manager.initialize();
    BuildTargetIdentifier btId = null;
    for (BuildTarget buildTarget : manager.getBuildTargets()) {
      if (buildTarget.getId().getUri().contains("module/submodule/?sourceset=main")) {
        btId = buildTarget.getId();
        break;
      }
    }
    
    assertNotNull(btId);
    GradleBuild gradleBuild = injector.getInstance(GradleBuild.class);
    assertEquals(StatusCode.OK, gradleBuild.build(Arrays.asList(btId)));
  }

  private void replaceContent(Path filePath, String contentToReplace,
      String replacement) throws IOException {
    String content = new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
    content = content.replace(contentToReplace, replacement);
    Files.write(filePath, content.getBytes(StandardCharsets.UTF_8));
  }
}
