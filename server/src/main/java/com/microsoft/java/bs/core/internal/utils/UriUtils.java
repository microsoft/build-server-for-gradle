package com.microsoft.java.bs.core.internal.utils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility class for URI.
 */
public class UriUtils {
  private UriUtils() {}

  /**
   * Get the URI from the given string.
   */
  public static URI getUriFromString(String uri) {
    try {
      return new URI(uri);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid rootUri: " + uri, e);
    }
  }
}
