// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core.internal.utils;

import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Utility class for JSON.
 */
public class JsonUtils {
  private JsonUtils() {}

  /**
   * Converts given JSON objects to given Model objects.
   *
   * @throws IllegalArgumentException if clazz is null
   */
  public static <T> T toModel(Object object, Class<T> clazz) {
    return toModel(new Gson(), object, clazz);
  }

  private static <T> T toModel(Gson gson, Object object, Class<T> clazz) {
    if (object == null) {
      return null;
    }
    if (clazz == null) {
      throw new IllegalArgumentException("Class can not be null");
    }
    if (object instanceof JsonElement json) {
      return gson.fromJson(json, clazz);
    }
    if (clazz.isInstance(object)) {
      return clazz.cast(object);
    }
    if (object instanceof String json) {
      return gson.fromJson(json, clazz);
    }
    return null;
  }
}
