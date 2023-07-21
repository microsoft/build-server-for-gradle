package com.microsoft.java.bs.core.internal.utils;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class UriUtilsTest {
  @Test
  void testGetUriFromString() {
    assertThrows(IllegalArgumentException.class, () -> {
      UriUtils.getUriFromString("!@#$%");
    });
  }
}
