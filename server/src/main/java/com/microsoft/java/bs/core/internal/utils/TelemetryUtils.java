// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Utils for telemetry.
 */
public class TelemetryUtils {
  private TelemetryUtils() {}

  /**
   * Return a map suitable for metadata telemetry.
   */
  public static Map<String, String> getMetadataMap(String kind, String data) {
    Map<String, String> map = new HashMap<>();
    map.put("kind", kind);
    map.put("data", data);
    return map;
  }
}
