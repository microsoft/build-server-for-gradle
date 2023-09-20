// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

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
      throw new IllegalArgumentException("Invalid uri: " + uri, e);
    }
  }

  /**
   * Returns the URI without query.
   */
  public static URI getUriWithoutQuery(String uriString) {
    try {
      URI uri = new URI(uriString);
      if (uri.getQuery() == null) {
        return uri;
      }

      return new URI(uri.getScheme(), uri.getHost(), uri.getPath(), null, uri.getFragment());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid uri: " + uriString, e);
    }
  }

  /**
   * Returns the query value by key from the URI.
   */
  public static String getQueryValueByKey(String uriString, String key) {
    try {
      URI uri = new URI(uriString);
      if (uri.getQuery() == null) {
        return "";
      }

      String query = uri.getQuery();
      String[] pairs = query.split("&");
      for (String pair : pairs) {
        int idx = pair.indexOf("=");
        if (idx > 0 && key.equals(pair.substring(0, idx))) {
          return pair.substring(idx + 1);
        }
      }

      return "";
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid uri: " + uriString, e);
    }
  }
}
