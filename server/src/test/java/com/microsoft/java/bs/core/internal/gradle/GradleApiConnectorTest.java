package com.microsoft.java.bs.core.internal.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
    System.setProperty("plugin.dir", pluginDir);
  }

  @Test
  void testGetGradleSourceSets() {
    File projectDir = projectPath.resolve("junit5-jupiter-starter-gradle").toFile();
    GradleApiConnector connector = new GradleApiConnector();
    GradleSourceSets gradleSourceSets = connector.getGradleSourceSets(projectDir.toURI());
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
