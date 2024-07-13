package com.microsoft.java.bs.core.internal.utils;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

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
  void testGetJavaVersionFromFileForException() throws IOException {

    ProcessBuilder mockBuilder = mock(ProcessBuilder.class);
    when(mockBuilder.redirectErrorStream(true)).thenReturn(mockBuilder);
    when(mockBuilder.start()).thenThrow(new IOException("Simulated Process Failure"));

    assertThrows(IOException.class, () -> getJavaVersionFromFile(mockBuilder));

  }

  @Test
  void testIsCompatibleValid() {

    String javaVersion = "17.0.3";
    String minJavaVersion = "17.0.1";
    String maxJavaVersion = "17.0.5";

    assertTrue(JavaUtils.isCompatible(javaVersion, minJavaVersion, maxJavaVersion));

  }

  @Test
  void testIsCompatibleMinInvalid() {

    String javaVersion = "11.0.1";
    String minJavaVersion = "17.0.1";
    String maxJavaVersion = "17.0.5";

    assertFalse(JavaUtils.isCompatible(javaVersion, minJavaVersion, maxJavaVersion));

  }

  @Test
  void testIsCompatibleMaxInvalid() {

    String javaVersion = "19.0.1";
    String minJavaVersion = "17.0.1";
    String maxJavaVersion = "17.0.5";

    assertFalse(JavaUtils.isCompatible(javaVersion, minJavaVersion, maxJavaVersion));

  }


}