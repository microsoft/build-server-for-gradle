// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT license.

package com.microsoft.java.bs.core;

import static com.microsoft.java.bs.core.Launcher.PROP_PLUGIN_DIR;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class LauncherTest {

  @Test
  void testSystemPropertyCheckForPluginLocation() {
    System.clearProperty(PROP_PLUGIN_DIR);
    assertThrows(IllegalStateException.class, () -> {
      Launcher.main(null);
    });
  }
}
