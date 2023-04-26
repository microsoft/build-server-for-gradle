package com.microsoft.java.bs.core.utils;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility class for URI.
 */
public class UriUtils {
  private UriUtils() {}

  /**
   * Returns the URI without query.
   */
  public static URI uriWithoutQuery(String uriString) throws URISyntaxException {
    URI uri = new URI(uriString);
    if (uri.getQuery() == null) {
      return uri;
    }

    return new URI(uri.getScheme(), uri.getHost(), uri.getPath(), null, uri.getFragment());
  }
}
