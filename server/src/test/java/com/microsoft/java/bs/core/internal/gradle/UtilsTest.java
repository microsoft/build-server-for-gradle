package com.microsoft.java.bs.core.internal.gradle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;

class UtilsTest {

  @Test
  void testGetFileFromProperty() {
    System.setProperty("test", "test");
    assertNotNull(Utils.getFileFromEnvOrProperty("test"));
  }

  @Test
  void testGetHighestCompatibleJavaVersion() {
    assertEquals("20", Utils.getHighestCompatibleJavaVersion("8.3"));
    assertEquals("11", Utils.getHighestCompatibleJavaVersion("5.2.4"));
    assertEquals("", Utils.getHighestCompatibleJavaVersion("1.0"));
  }

  @Test
  void testGetGradleVersion() {
    File projectDir = Paths.get(
        System.getProperty("user.dir"),
        "..",
        "testProjects",
        "gradle-4.3-with-wrapper"
    ).normalize().toFile();

    assertEquals("4.3", Utils.getGradleVersion(projectDir.toURI()));
  }
}
