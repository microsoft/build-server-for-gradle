package com.microsoft.java.bs.core.internal.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static com.microsoft.java.bs.core.internal.utils.JavaUtils.getJavaVersionFromFile;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class JavaUtilsTest {

  @Test
  void testGetJavaVersionFromFile() throws IOException {

    String expectedVersion = "java version \"17.0.1\"";

    Process mockProcess = mock(Process.class);
    InputStream mockStream = new ByteArrayInputStream(expectedVersion.getBytes());
    when(mockProcess.getInputStream()).thenReturn(mockStream);

    ProcessBuilder mockBuilder = mock(ProcessBuilder.class);
    when(mockBuilder.redirectErrorStream(true)).thenReturn(mockBuilder);
    when(mockBuilder.start()).thenReturn(mockProcess);
    String actualVersion = getJavaVersionFromFile(mockBuilder);

    assertEquals(expectedVersion.split("\"")[1], actualVersion);

    verify(mockProcess).destroy();

  }

  @Test
  void testGetJavaVersionFromFile_SimulatedException() throws IOException {

    ProcessBuilder mockBuilder = mock(ProcessBuilder.class);
    when(mockBuilder.redirectErrorStream(true)).thenReturn(mockBuilder);
    when(mockBuilder.start()).thenThrow(new IOException("Simulated Process Failure"));

    assertThrows(IOException.class, () -> getJavaVersionFromFile(mockBuilder));

  }

  @Test
  void testGetJavaVersionFromFile_NonExistentExecutable() throws IOException {

    Path tempDirectory = Files.createTempDirectory("test-jdk");
    ProcessBuilder processBuilder =
        new ProcessBuilder(tempDirectory.toString() + "/bin/java", "-version");

    assertThrows(IOException.class, () -> getJavaVersionFromFile(processBuilder));
    Files.deleteIfExists(tempDirectory);

  }

  @Test
  void testIsCompatible_Valid() {

    String javaVersion = "17.0.3";
    String oldestVersion = "17.0.1";
    String latestVersion = "17.0.5";

    assertTrue(JavaUtils.isCompatible(javaVersion, oldestVersion, latestVersion));

  }

  @Test
  void testIsCompatible_OldestInvalid() {

    String javaVersion = "11.0.1";
    String oldestVersion = "17.0.1";
    String latestVersion = "17.0.5";

    assertFalse(JavaUtils.isCompatible(javaVersion, oldestVersion, latestVersion));

  }

  @Test
  void testIsCompatible_LatestInvalid() {

    String javaVersion = "19.0.1";
    String oldestVersion = "17.0.1";
    String latestVersion = "17.0.5";

    assertFalse(JavaUtils.isCompatible(javaVersion, oldestVersion, latestVersion));

  }


}