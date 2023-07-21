package com.microsoft.java.bs.core.internal.gradle;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class UtilsTest {

  @Test
  void testGetFileFromProperty() {
    System.setProperty("test", "test");
    assertNotNull(Utils.getFileFromEnvOrProperty("test"));
  }
}
