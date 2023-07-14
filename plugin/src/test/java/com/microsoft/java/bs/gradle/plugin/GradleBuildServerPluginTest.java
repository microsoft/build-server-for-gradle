package com.microsoft.java.bs.gradle.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    ProjectConnection connect = connector.connect();
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
    }
  }
}
