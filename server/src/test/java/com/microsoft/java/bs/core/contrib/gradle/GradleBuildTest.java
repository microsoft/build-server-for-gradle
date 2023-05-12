package com.microsoft.java.bs.core.contrib.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTarget;
import com.microsoft.java.bs.contrib.gradle.model.JavaBuildTargets;
import com.microsoft.java.bs.core.BspModule;

import ch.epfl.scala.bsp4j.BuildTargetIdentifier;
import ch.epfl.scala.bsp4j.StatusCode;

/**
 * Tests for {@link GradleBuild}.
 */
class GradleBuildTest {

  private static File projectDir;

  @BeforeAll
  static void beforeClass() {
    projectDir = Paths.get(
        System.getProperty("user.dir"),
        "..",
        "testProjects",
        "junit5-jupiter-starter-gradle"
    ).toFile();
  }

  @Test
  void testGetSourceSetEntries() throws Exception {
    Injector injector = Guice.createInjector(new BspModule());
    GradleBuild gradleBuild = injector.getInstance(GradleBuild.class);
    JavaBuildTargets sourceSetEntries = gradleBuild.getSourceSetEntries(projectDir.toURI());
    List<JavaBuildTarget> javaBuildTargets = sourceSetEntries.getJavaBuildTargets();

    assertEquals(2, javaBuildTargets.size());
    for (JavaBuildTarget javaBuildTarget : javaBuildTargets) {
      assertEquals(1, javaBuildTarget.getSourceDirs().size());
      if (Objects.equals(javaBuildTarget.getSourceSetName(), "test")) {
        assertEquals(6, javaBuildTarget.getModuleDependencies().size());
        assertEquals(0, javaBuildTarget.getProjectDependencies().size());
      }
    }
  }

  @Test
  void testCompilationFailure() throws IOException {
    try {
      replaceContent("return a + b;", "return a + b");

      Injector injector = Guice.createInjector(new BspModule());
      GradleBuild gradleBuild = injector.getInstance(GradleBuild.class);
      BuildTargetIdentifier btId = new BuildTargetIdentifier(projectDir.toURI().toString());
      assertEquals(StatusCode.ERROR, gradleBuild.build(Arrays.asList(btId)));
    } finally {
      replaceContent("return a + b", "return a + b;");
    }
  }

  private void replaceContent(String contentToReplace, String replacement) throws IOException {
    Path path = projectDir.toPath().resolve("src/main/java/com/example/project/Calculator.java");
      
    String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    content = content.replaceAll(contentToReplace, replacement);
    Files.write(path, content.getBytes(StandardCharsets.UTF_8));
  }
}
