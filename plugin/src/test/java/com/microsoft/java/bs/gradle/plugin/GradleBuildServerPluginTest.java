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
        assertTrue(gradleSourceSet.getSourceDirs().size() > 0);
        assertTrue(gradleSourceSet.getGeneratedSourceDirs().size() > 0);
        assertTrue(gradleSourceSet.getResourceDirs().size() > 0);
        assertNotNull(gradleSourceSet.getSourceOutputDir());
        assertNotNull(gradleSourceSet.getResourceOutputDir());

        assertNotNull(gradleSourceSet.getJavaHome());
        assertNotNull(gradleSourceSet.getJavaVersion());
        assertNotNull(gradleSourceSet.getSourceCompatibility());
        assertNotNull(gradleSourceSet.getTargetCompatibility());
        assertNotNull(gradleSourceSet.getGradleVersion());
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
