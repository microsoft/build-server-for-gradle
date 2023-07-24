package com.microsoft.java.bs.core.internal.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.URISyntaxException;

import org.junit.jupiter.api.Test;

class UriUtilsTest {
  @Test
  void testGetUriFromString() {
    assertThrows(IllegalArgumentException.class, () -> {
      UriUtils.getUriFromString("!@#$%");
    });
  }

  @Test
  void testGetQueryValueByKey() {
    String uriString = "file:/C:/Users/foo/bar?a=b&c=d";
    assertEquals("b", UriUtils.getQueryValueByKey(uriString, "a"));
    assertEquals("d", UriUtils.getQueryValueByKey(uriString, "c"));
    assertTrue(UriUtils.getQueryValueByKey(uriString, "foo").isEmpty());
  }

  @Test
  void testGetUriWithoutQuery() throws URISyntaxException {
    String uriString = "file:/C:/Users/foo/bar?a=b&c=d";
    URI expected = new URI("file:/C:/Users/foo/bar");
    assertEquals(expected, UriUtils.getUriWithoutQuery(uriString));
  }
}
